package uk.ac.cam.seh208.middleware.core;


/**
 * An interface to a socket context, which maintains the state necessary to open
 * and maintain MessageStream objects. Message streams allow two-way communication
 * to remote instances of the middleware that is message delimited, asynchronous,
 * and internally queued.
 */
public interface MessageContext {
    // TODO: use different addressing scheme for versatility.
    /**
     * Return a socket for communication with a particular remote middleware. The
     * socket must be ready for use, at least opaquely via set-up procedures run
     * on the first message in either direction. Multiple calls for the same host
     * must return a reference to the same socket.
     *
     * @param host Host on which the remote middleware instance resides.
     *
     * @return a reference to a MessageStream object.
     */
    MessageStream getStream(String host);
}
