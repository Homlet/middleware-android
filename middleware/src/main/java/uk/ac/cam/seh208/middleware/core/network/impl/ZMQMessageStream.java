package uk.ac.cam.seh208.middleware.core.network.impl;

import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.seh208.middleware.core.CloseableSubject;
import uk.ac.cam.seh208.middleware.core.network.Address;
import uk.ac.cam.seh208.middleware.core.network.MessageListener;
import uk.ac.cam.seh208.middleware.core.network.MessageStream;


/**
 * ZeroMQ Harmony-pattern implementor of the message stream interface.
 */
public class ZMQMessageStream extends CloseableSubject<ZMQMessageStream>
        implements MessageStream, MessageListener {

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
    private Address localAddress;

    /**
     * The ZeroMQ address with which this stream communicates.
     */
    private Address remoteAddress;

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
        // TODO: send FIN message to remote host.

        // If the DEALER socket was already created, close and release it.
        if (dealer != null) {
            dealer.close();
            dealer = null;
        }

        super.close();
    }

    @Override
    public synchronized void send(String message) {
        // We cannot send from a closed stream.
        if (isClosed()) {
            return;
        }

        // If the DEALER socket has not been opened, do so now.
        if (dealer == null) {
            if (!connect()) {
                return;
            }

            // The new socket has been created and connected successfully.
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
        if (listener == this) {
            // Don't let the object listen to itself; this would create a feedback loop.
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
        for (MessageListener listener : listeners) {
            listener.onMessage(message);
        }
    }

    /**
     * Open the DEALER socket to the peer if this has not already been done. After
     * opening, an initialisation message is sent containing the local host.
     *
     * @return whether the DEALER socket exists and is connected to the peer. If not,
     *         the dealer field will remain null.
     */
    private synchronized boolean connect() {
        // We cannot re-establish a DEALER after the stream has been closed.
        if (isClosed()) {
            return false;
        }

        // If the DEALER socket has already been opened, do not open another.
        if (dealer != null) {
            return true;
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
            // The attempt failed. Close and release the new socket.
            if (dealer != null) {
                dealer.close();
                dealer = null;
            }
            return false;
        }

        return true;
    }
}
