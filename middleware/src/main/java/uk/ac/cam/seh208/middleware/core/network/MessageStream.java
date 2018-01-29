package uk.ac.cam.seh208.middleware.core.network;

import uk.ac.cam.seh208.middleware.core.Closeable;


/**
 * Simple interface for an asynchronous, first-in-first-out, message delimited stream socket.
 */
public interface MessageStream extends Closeable {
    /**
     * Queue a string message to be sent from the socket asynchronously.
     *
     * The socket will handle message delimitation and queueing.
     *
     * @param message Complete string message to send over the socket.
     */
    void send(String message);

    /**
     * Register a listener to be run on receipt of a new message.
     *
     * @param listener A MessageListener implementor to be registered.
     */
    void registerListener(MessageListener listener);

    /**
     * Unregister a currently registered message listener.
     *
     * @param listener A MessageListener implementor to be unregistered.
     */
    void unregisterListener(MessageListener listener);

    /**
     * Unregister all currently registered message listeners.
     */
    void clearListeners();
}
