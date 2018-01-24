package uk.ac.cam.seh208.middleware.core.network;


/**
 * An interface to a request context, which maintains the state necessary to send
 * asynchronous requests to other middleware instances reliably. Requests are used
 * to implement control messages, which coordinate mappings and channels between
 * middlewares, as well as run commands on remote instances of the middleware.
 */
public interface RequestContext {
    // TODO: use different addressing scheme for versatility.
    /**
     * Return a request stream to a remote instance of the middleware. The stream
     * must be ready for sending asynchronous requests to the remote host.
     *
     * @param host Host on which the remote middleware instance resides.
     *
     * @return a reference to a RequestStream object.
     */
    RequestStream getRequestStream(String host);

    /**
     * Return the responder handling requests received via this context. The responder
     * must queue all incoming requests until a handler is set by the user, at which
     * point it will service requests by calling the handler method, and sending the
     * return value to the peer.
     *
     * @return a reference to a Responder object.
     */
    Responder getResponder();
}
