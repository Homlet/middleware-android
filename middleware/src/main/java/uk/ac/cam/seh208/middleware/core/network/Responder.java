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
public class Responder {

    /**
     * Request handler interface implementing the middleware-level response.
     */
    private RequestHandler handler;


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
    public synchronized void setHandler(RequestHandler handler) {
        if (handler == null) {
            // We do not allow the handler to be set to null;
            return;
        }

        // Set the new handler.
        this.handler = handler;

        // Wake threads waiting for a non-null handler.
        notifyAll();
    }

    /**
     * Respond to a request, blocking until the middleware layer handler has
     * returned with a response. If the handler is not yet set, wait until it is.
     *
     * @param request The string formatted request body.
     *
     * @return the string response from the middleware layer.
     */
    public String respond(String request) {
        RequestHandler handler;
        synchronized (this) {
            // Grab the first non-null version of the handler. Note that
            // the handler can never be set null again after being set.
            if (this.handler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // The thread has been interrupted; return control flow
                    // to the caller.
                    return null;
                }
            }
            handler = this.handler;
        }

        // Respond without holding the lock, so multiple threads may use the
        // responder at once.
        return handler.respond(request);
    }
}
