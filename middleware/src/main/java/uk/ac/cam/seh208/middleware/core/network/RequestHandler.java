package uk.ac.cam.seh208.middleware.core.network;


/**
 * Interface for a handler of asynchronous requests. Request numbers
 * and peer addressing is handled by the invoker, leaving the implementor
 * only to service the request and return a response.
 */
public interface RequestHandler {

    /**
     * Invoked by the responder when an incoming request is received,
     * either returning a string response, or returning null to indicate error.
     *
     * @return the response to the request.
     */
    String respond(String request);
}
