package uk.ac.cam.seh208.middleware.core.network;


/**
 * Simple interface for an asynchronous server. Requests from peers are
 * internally buffered until a handler is set, at which point the handler
 * method is invoked for each incoming request, and the return value sent to
 * the peer as the response.
 *
 * The interface does not determine the number of threads available for
 * handler invocation, so the user must take care to ensure that the handler
 * method is thread safe.
 */
public interface Responder {
    /**
     * Set a new handler for responding to requests. The handler method
     * must return a single string as the response, which will be sent to
     * the original requester. The handler method may be invoked concurrently
     * on many worker threads. Requests arriving before the initial handler
     * is set are buffered. The handler may subsequently be changed using
     * this method and all future requests will use the new handler.
     *
     * @param handler RequestHandler implementor for servicing incoming requests.
     */
    void setHandler(RequestHandler handler);
}
