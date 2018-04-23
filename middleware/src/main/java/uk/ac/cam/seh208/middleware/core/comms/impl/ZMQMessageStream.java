package uk.ac.cam.seh208.middleware.core.comms.impl;

import android.util.Log;
import android.util.LongSparseArray;

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

    private class DealerThread extends Thread {

        /**
         * The amount of time (in milliseconds) that the internal and external
         * sockets should remain open after a close call if they still
         * have messages queueing.
         */
        private static final int SOCKET_LINGER = 200;


        /**
         * DEALER socket that sends messages to the remote Harmony server.
         */
        private ZMQ.Socket dealerExternal;

        /**
         * DEALER socket that receives messages from other threads within
         * the process to forward along the external DEALER socket.
         */
        private ZMQ.Socket dealerInternal;


        @Override
        public void run() {
            try {
                // Open a new DEALER socket.
                dealerExternal = context.socket(ZMQ.DEALER);
                dealerExternal.setLinger(SOCKET_LINGER);
                dealerExternal.setSendTimeOut(SOCKET_LINGER);

                // Attempt to connect the socket to the peer.
                dealerExternal.connect("tcp://" + remote.toAddressString());

                // Attempt to send the initial message to the peer.
                ZMQInitialMessage message = new ZMQInitialMessage(environment.getLocation());
                dealerExternal.send(message.toJSON());
            } catch (ZMQException e) {
                // The attempt failed. Close the new socket if open.
                if (dealerExternal != null) {
                    dealerExternal.close();
                }

                // Close the message stream and return.
                close();
                return;
            }

            // Create a new internal and control dealer socket.
            dealerInternal = context.socket(ZMQ.DEALER);
            dealerInternal.setLinger(SOCKET_LINGER);
            dealerInternal.setReceiveTimeOut(SOCKET_LINGER);
            dealerInternal.bind("inproc://dealer_" + streamId);

            // Forward internal messages to the external dealer socket via a proxy.
            try {
                while (!isClosed()) {
                    byte[] msg = dealerInternal.recv();
                    if (msg == null) {
                        // The recv call timed out. Check the loop condition and continue.
                        continue;
                    }
                    dealerExternal.send(msg);
                }

                // Send the FIN message.
                dealerExternal.send("");
            } catch (ZMQException e) {
                if (e.getErrorCode() != ZMQ.Error.ETERM.getCode()) {
                    // This was not thrown due to context termination.
                    Log.e(getTag(), "Fatal error in message server", e);
                }
            } finally {
                // Close the sockets.
                dealerInternal.close();
                dealerExternal.close();
            }
        }
    }


    /**
     * Process-local identifier for the stream used to link local queues to the dealer thread.
     */
    private final long streamId;

    /**
     * Reference to the environment owning this stream.
     */
    private final Environment environment;

    /**
     * ZeroMQ context in which to open the DEALER socket.
     */
    private final ZMQ.Context context;

    /**
     * The ZeroMQ address with which this stream communicates.
     */
    private final ZMQAddress remote;

    /**
     * The single thread responsible for sending queued messages out via the
     * external dealer socket.
     */
    private final DealerThread dealerThread;

    /**
     * Thread-local message queue to the dealer thread.
     */
    private final LongSparseArray<ZMQ.Socket> queues;

    /**
     * Read-write lock protecting the queues map.
     */
    private final ReadWriteLock queuesLock;

    /**
     * Collection of listeners used to respond to messages.
     */
    private final List<MessageListener> listeners;


    ZMQMessageStream(Environment environment, ZMQ.Context context, ZMQAddress remote) {
        streamId = getNextStreamId();
        this.environment = environment;
        this.context = context;
        this.remote = remote;
        dealerThread = new DealerThread();
        queues = new LongSparseArray<>();
        queuesLock = new ReentrantReadWriteLock(true);
        listeners = new ArrayList<>();

        // Start the stream dealer thread.
        dealerThread.start();
    }

    @Override
    public void close() {
        synchronized (this) {
            // Don't resend the FIN message if we're already closed.
            if (isClosed()) {
                return;
            }

            // Acquire the queue write lock (method-level synchronisation
            // ensures that there is no writer-writer contention).
            queuesLock.writeLock().lock();

            // Get and close all message queues to the dealer thread.
            int size = queues.size();
            for (int i = 0; i < size; i++) {
                queues.valueAt(i).close();
                queues.removeAt(i);
            }

            // Signal the server to stop tracking this stream for the remote address.
            super.close();

            // Release the queue state lock.
            queuesLock.writeLock().unlock();
        }

        try {
            dealerThread.join();
        } catch (InterruptedException e) {
            Log.e(getTag(), "Thread interrupted while joining with the dealer thread.");
        }
    }

    @Override
    public void send(String message) throws ConnectionFailedException {
        // Acquire the queue read lock.
        queuesLock.readLock().lock();

        // We cannot send from a closed stream.
        if (isClosed()) {
            return;
        }

        // Get the message queue to the dealer thread for the current thread.
        ZMQ.Socket queue = getQueue();

        // Send the message to the dealer thread.
        queue.send(message);

        // Release the queue state lock.
        queuesLock.readLock().unlock();
    }

    /**
     * @return the calling thread's message queue to the dealer thread.
     */
    private ZMQ.Socket getQueue() {
        long threadId = Thread.currentThread().getId();
        if (queues.get(threadId) == null) {
            ZMQ.Socket queue = context.socket(ZMQ.DEALER);
            queue.connect("inproc://dealer_" + streamId);
            queues.put(threadId, queue);
        }

        return queues.get(threadId);
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
            Log.d(getTag(), "Dropped message \"" + message + "\"");
            return;
        }

        // Dispatch the message to all registered listeners.
        for (MessageListener listener : listeners) {
            listener.onMessage(message);
        }
    }

    private static long nextStreamId = 0;

    private static synchronized long getNextStreamId() {
        return nextStreamId++;
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
