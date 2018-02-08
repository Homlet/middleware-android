package uk.ac.cam.seh208.middleware.core.network.impl;

import android.util.Log;

import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.seh208.middleware.core.CloseableSubject;
import uk.ac.cam.seh208.middleware.core.exception.ConnectionFailedException;
import uk.ac.cam.seh208.middleware.core.network.MessageListener;
import uk.ac.cam.seh208.middleware.core.network.MessageStream;


/**
 * ZeroMQ Harmony-pattern implementor of the message stream interface.
 */
public class ZMQMessageStream extends CloseableSubject<ZMQMessageStream> implements MessageStream {


    /**
     * DEALER socket over which to send new messages.
     */
    private ZMQ.Socket dealer;

    /**
     * ZeroMQ context in which to open the DEALER socket.
     */
    private ZMQ.Context context;

    // TODO: store a full location.
    /**
     * The ZeroMQ address on which the Harmony ROUTER socket resides.
     */
    private ZMQAddress localAddress;

    /**
     * The ZeroMQ address with which this stream communicates.
     */
    private ZMQAddress remoteAddress;

    /**
     * Collection of listeners used to respond to messages.
     */
    private List<MessageListener> listeners;


    public ZMQMessageStream(ZMQ.Context context, ZMQAddress localAddress,
                            ZMQAddress remoteAddress) {
        this.context = context;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        listeners = new ArrayList<>();
    }

    @Override
    public synchronized void close() {
        // Don't resend the FIN message if we're already closed.
        if (isClosed()) {
            return;
        }

        try {
            // Connect to the peer (if not already) to send the FIN message.
            connect();
            dealer.send("");
            disconnect();
        } catch (ConnectionFailedException ignored) {
            // This is very unlikely to happen due to the way ZeroMQ
            // defers TCP connections.
        }

        // Signal the server to stop tracking this stream for the remote address.
        super.close();
    }

    @Override
    public synchronized void send(String message) throws ConnectionFailedException {
        // We cannot send from a closed stream.
        if (isClosed()) {
            return;
        }

        // If the DEALER socket has not been opened, do so now.
        if (dealer == null) {
            connect();
        }

        // Send the message over the DEALER socket.
        dealer.send(message);
    }

    /**
     * Register a new message listener with the message stream. Messages received
     * after registering will be passed to this new listener along will all others
     * registered.
     *
     * @param listener A MessageListener implementor to be registered.
     */
    @Override
    public synchronized void registerListener(MessageListener listener) {
        // Don't allow registering of listeners after the stream has closed.
        if (isClosed()) {
            return;
        }

        // Don't let the object listen to itself; this would create a feedback loop.
        if (listener == this) {
            return;
        }

        listeners.add(listener);
    }

    /**
     * Remove an existing message listener from the message stream. Messages
     * received after unregistering will no longer be passing to this listener,
     * but will continue to be dispatched to all others registered.
     *
     * @param listener A MessageListener implementor to be unregistered.
     */
    @Override
    public synchronized void unregisterListener(MessageListener listener) {
        listeners.remove(listener);
    }

    /**
     * Remove all existing message listeners from the message stream. Messages
     * received after clearing will be dropped.
     */
    @Override
    public synchronized void clearListeners() {
        listeners.clear();
    }

    /**
     * Dispatch a received message to all currently registered listeners.
     *
     * @param message The newly received string message.
     */
    public synchronized void onMessage(String message) {
        // If we are closed, all messages should be ignored. Eventually
        // FIN will be received and we can release this object.
        if (isClosed()) {
            Log.d(getTag(), "DROP: \"" + message + "\"");
            return;
        }

        Log.d(getTag(), "MSG: \"" + message + "\"");

        // Dispatch the message to all registered listeners.
        for (MessageListener listener : listeners) {
            listener.onMessage(message);
        }
    }

    public ZMQAddress getLocalAddress() {
        return localAddress;
    }

    public ZMQAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Open the DEALER socket to the peer if this has not already been done. After
     * opening, an initialisation message is sent containing the local host.
     */
    private synchronized void connect() throws ConnectionFailedException {
        // We cannot re-establish a DEALER after the stream has been closed.
        if (isClosed()) {
            return; //throw new ConnectionFailedException(remoteAddress);
        }

        // If the DEALER socket has already been opened, do not open another.
        if (dealer != null) {
            return;
        }

        try {
            // Open a new DEALER socket.
            dealer = context.socket(ZMQ.DEALER);

            // Attempt to connect the socket to the peer.
            dealer.connect("tcp://" + remoteAddress.toCanonicalString());

            // Attempt to send the initial message to the peer.
            // TODO: send complete location string rather than address.
            dealer.send(localAddress.toCanonicalString());
        } catch (ZMQException e) {
            // The attempt failed. Close and release the new socket if open.
            if (dealer != null) {
                dealer.close();
                dealer = null;
            }

            throw new ConnectionFailedException(remoteAddress);
        }
    }

    /**
     * Close the DEALER socket to the peer if it is currently open, breaking
     * the connection to the peer.
     */
    private synchronized void disconnect() {
        // If the DEALER does not exist, there's nothing to close.
        if (dealer == null) {
            return;
        }

        // Close and release the socket.
        dealer.close();
        dealer = null;
    }

    String getTag() {
        return "STREAM[" + localAddress + " | " + remoteAddress + "]";
    }
}
