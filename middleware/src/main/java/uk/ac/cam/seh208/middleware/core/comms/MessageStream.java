package uk.ac.cam.seh208.middleware.core.comms;

import uk.ac.cam.seh208.middleware.core.CloseableSubject;
import uk.ac.cam.seh208.middleware.core.exception.ConnectionFailedException;


/**
 * Simple interface for an asynchronous, first-in-first-out, message delimited stream socket.
 *
 * A simple implementation might maintain a single underlying connection to the remote host,
 * but there is scope for establishing multiple connections to particular hosts for load
 * balancing purposes; i.e. to leverage threading on the device when connections host channels
 * to different endpoints.
 */
public abstract class MessageStream extends CloseableSubject<MessageStream>
        implements MessageListener {

    /**
     * Queue a string message to be sent from the socket asynchronously.
     *
     * The socket will handle message delimitation and queueing.
     *
     * @param message Complete string message to send over the socket.
     */
    public abstract void send(String message) throws ConnectionFailedException;

    /**
     * Register a listener to be run on receipt of a new message.
     *
     * @param listener A MessageListener implementor to be registered.
     */
    public abstract void registerListener(MessageListener listener);

    /**
     * Unregister a currently registered message listener.
     *
     * @param listener A MessageListener implementor to be unregistered.
     */
    public abstract void unregisterListener(MessageListener listener);

    /**
     * Unregister all currently registered message listeners.
     */
    public abstract void clearListeners();
}
