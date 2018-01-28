package uk.ac.cam.seh208.middleware.core.network;

import uk.ac.cam.seh208.middleware.core.Closeable;


/**
 * Simple interface for an asynchronous request socket with blocking semantics
 * for responses.
 */
public interface RequestStream extends Closeable {
    /**
     * Send a request message to the remote host, blocking until a response
     * is received. Should the stream be closed, or the remote host respond
     * with an error, null is returned immediately.
     *
     * @param request String request to send to the remote middleware instance.
     *
     * @return the response string, or null in the case of an error.
     */
    String request(String request);
}
