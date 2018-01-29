package uk.ac.cam.seh208.middleware.core.network.impl;

import uk.ac.cam.seh208.middleware.core.network.RequestHandler;
import uk.ac.cam.seh208.middleware.core.network.Responder;

/**
 * ZMQ implementor of the responder interface.
 */
public class ZMQResponder implements Responder {

    /**
     * Request handler interface implementing the middleware-level response.
     */
    private RequestHandler handler;


    /**
     * Set the request handler to something other than null.
     *
     * @param handler RequestHandler implementor for servicing incoming requests.
     */
    @Override
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
        synchronized (this) {
            // Grab the first non-null version of the handler. Note that
            // the handler can never be set null again after being set.
            if (handler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // The thread has been interrupted; return control flow
                    // to the caller.
                    return null;
                }
            }
            RequestHandler handler = this.handler;
        }

        // Respond without holding the lock, so multiple threads may use the
        // responder at once.
        return handler.respond(request);
    }
}
