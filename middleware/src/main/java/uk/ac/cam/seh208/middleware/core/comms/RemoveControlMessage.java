package uk.ac.cam.seh208.middleware.core.comms;

import android.app.Service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import uk.ac.cam.seh208.middleware.core.RDCService;
import uk.ac.cam.seh208.middleware.core.network.RequestStream;


/**
 * Control message sent to an RDC to remove a middleware instance from the record.
 */
public class RemoveControlMessage extends ControlMessage {

    /**
     * The response is simply an acknowledgement.
     */
    public static class Response extends ControlMessage.Response {

        private static Response instance;


        public static Response getInstance() {
            if (instance == null) {
                instance = new Response();
            }
            return instance;
        }


        private Response() { }

        @Override
        public boolean equals(Object obj) {
            return this == obj || obj != null && getClass() == obj.getClass();
        }
    }


    /**
     * Object describing the middleware instance.
     */
    private Middleware middleware;


    /**
     * Instantiate a new immutable REMOVE control message for the given location.
     */
    public RemoveControlMessage(@JsonProperty("middleware") Middleware middleware) {
        this.middleware = middleware;
    }

    /**
     * Remove the entry from the RDC state, and respond with acknowledgement.
     *
     * @param service A reference to the RDC service receiving the message.
     *
     * @return an acknowledgement response.
     */
    @Override
    public Response handle(Service service) {
        if (!(service instanceof RDCService)) {
            // REMOVE can only be handled by an RDC instance.
            return null;
        }

        RDCService rdc = (RDCService) service;
        rdc.remove(middleware);

        return Response.getInstance();
    }

    @Override
    public RemoveControlMessage.Response getResponse(RequestStream stream) {
        return (RemoveControlMessage.Response) super.getResponse(stream);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RemoveControlMessage other = (RemoveControlMessage) obj;

        return Objects.equals(middleware, other.middleware);
    }
}
