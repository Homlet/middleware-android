package uk.ac.cam.seh208.middleware.core.comms.impl;

import android.util.Log;

import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import uk.ac.cam.seh208.middleware.core.exception.ConnectionFailedException;
import uk.ac.cam.seh208.middleware.core.comms.Environment;
import uk.ac.cam.seh208.middleware.core.comms.MessageListener;
import uk.ac.cam.seh208.middleware.core.comms.MessageStream;
import uk.ac.cam.seh208.middleware.core.exception.NoValidAddressException;


/**
 * ZeroMQ Harmony-pattern implementor of the message stream interface.
 */
public class ZMQMessageStream extends MessageStream {

    /**
     * Reference to the environment owning this stream.
     */
    private final Environment environment;

    /**
     * DEALER socket over which to send new messages.
     */
    private ZMQ.Socket dealer;

    /**
     * ZeroMQ context in which to open the DEALER socket.
     */
    private final ZMQ.Context context;

    /**
     * The ZeroMQ address with which this stream communicates.
     */
    private final ZMQAddress remote;

    /**
     * Collection of listeners used to respond to messages.
     */
    private final List<MessageListener> listeners;

    /**
     * Lock used to prevent handlers of sending and receiving from seeing partially
     * updated multiplexer state.
     */
    private final ReadWriteLock stateLock;


    ZMQMessageStream(Environment environment, ZMQ.Context context, ZMQAddress remote) {
        this.environment = environment;
        this.context = context;
        this.remote = remote;
        listeners = new ArrayList<>();
        stateLock = new ReentrantReadWriteLock(true);
    }

    @Override
    public synchronized void close() {
        // Don't resend the FIN message if we're already closed.
        if (isClosed()) {
            return;
        }

        // Connect to the peer (if not already) to send the FIN message.
        try {
            // Acquire the state write lock.
            stateLock.readLock().lock();

            while (dealer == null) {
                // Release the read lock around the connect call, so that connect
                // can attain the write lock.
                stateLock.readLock().unlock();
                connect();
                stateLock.readLock().lock();

                // TODO: throw after n failed attempts.
            }

            // Send the message over the DEALER socket.
            dealer.send("");

            // Release the state lock.
            stateLock.readLock().unlock();

            disconnect();
        } catch (ConnectionFailedException ignored) {
            // This is very unlikely to happen due to the way ZeroMQ
            // defers TCP connections.
        }

        // Signal the server to stop tracking this stream for the remote address.
        super.close();
    }

    @Override
    public void send(String message) throws ConnectionFailedException {
        // We cannot send from a closed stream.
        if (isClosed()) {
            return;
        }

        // Acquire the state write lock.
        stateLock.readLock().lock();

        // If the DEALER socket has not been opened, do so now.
        while (dealer == null) {
            // Release the read lock around the connect call, so that connect
            // can attain the write lock. The connect call will double check
            // the dealer is still null before proceeding, so there is no race
            // condition with other send calls having reached this point.
            stateLock.readLock().unlock();
            connect();
            stateLock.readLock().lock();

            // TODO: throw after n failed attempts.
        }

        // Send the message over the DEALER socket.
        boolean success = dealer.send(message);

        if (!success) {
            Log.w(getTag(), "Failed to send: \"" + message + "\"");
        }

        // Release the state lock.
        stateLock.readLock().unlock();
    }

    /**
     * Register a new message listener with the message stream. Messages received
     * after registering will be passed to this new listener along will all others
     * registered.
     *
     * @param listener A MessageListener implementor to be registered.
     */
    @Override
    public void registerListener(MessageListener listener) {
        // Don't allow registering of listeners after the stream has closed.
        if (isClosed()) {
            return;
        }

        // Don't let the object listen to itself; this would create a feedback loop.
        if (listener == this) {
            return;
        }

        stateLock.writeLock().lock();
        listeners.add(listener);
        stateLock.writeLock().unlock();
    }

    /**
     * Remove an existing message listener from the message stream. Messages
     * received after unregistering will no longer be passing to this listener,
     * but will continue to be dispatched to all others registered.
     *
     * @param listener A MessageListener implementor to be unregistered.
     */
    @Override
    public void unregisterListener(MessageListener listener) {
        stateLock.writeLock().lock();
        listeners.remove(listener);
        stateLock.writeLock().unlock();
    }

    /**
     * Remove all existing message listeners from the message stream. Messages
     * received after clearing will be dropped.
     */
    @Override
    public void clearListeners() {
        stateLock.writeLock().lock();
        listeners.clear();
        stateLock.writeLock().unlock();
    }

    /**
     * Dispatch a received message to all currently registered listeners.
     *
     * @param message The newly received string message.
     */
    public void onMessage(String message) {
        // If we are closed, all messages should be ignored. Eventually
        // FIN will be received and we can release this object.
        if (isClosed()) {
            Log.d(getTag(), "Dropped message \"" + message + "\"");
            return;
        }

        // Acquire the state read lock.
        stateLock.readLock().lock();

        // Dispatch the message to all registered listeners.
        for (MessageListener listener : listeners) {
            listener.onMessage(message);
        }

        // Release the state lock.
        stateLock.readLock().unlock();
    }

    /**
     * Open the DEALER socket to the peer if this has not already been done. After
     * opening, an initialisation message is sent containing the local host.
     */
    private void connect() throws ConnectionFailedException {
        // We cannot re-establish a DEALER after the stream has been closed.
        if (isClosed()) {
            return; //throw new ConnectionFailedException(remoteAddress);
        }

        stateLock.writeLock().lock();

        // If the DEALER socket has already been opened, do not open another.
        if (dealer != null) {
            return;
        }

        try {
            // Open a new DEALER socket.
            dealer = context.socket(ZMQ.DEALER);
            dealer.setSendTimeOut(0);

            // Attempt to connect the socket to the peer.
            dealer.connect("tcp://" + remote.toAddressString());

            // Attempt to send the initial message to the peer.
            ZMQInitialMessage message = new ZMQInitialMessage(environment.getLocation());
            dealer.send(message.toJSON());
        } catch (ZMQException e) {
            // The attempt failed. Close and release the new socket if open.
            if (dealer != null) {
                dealer.close();
                dealer = null;
            }

            throw new ConnectionFailedException(remote);
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    /**
     * Close the DEALER socket to the peer if it is currently open, breaking
     * the connection to the peer.
     */
    private void disconnect() {
        stateLock.writeLock().lock();

        // If the DEALER does not exist, there's nothing to close.
        if (dealer == null) {
            return;
        }

        // Close and release the socket.
        dealer.close();
        dealer = null;

        stateLock.writeLock().unlock();
    }

    ZMQAddress getRemote() {
        return remote;
    }

    String getTag() {
        try {
            return "STREAM[" + environment.getLocation().priorityAddress() + " > " + remote + "]";
        } catch (NoValidAddressException e) {
            return "STREAM[ > " + remote + "]";
        }
    }
}
