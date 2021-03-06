package uk.ac.cam.seh208.middleware.core.control;

import android.app.Service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.core.RDCService;
import uk.ac.cam.seh208.middleware.core.comms.RequestStream;


/**
 * Control message sent to an RDC to update the list of endpoints exposed by
 * the local middleware, or to register in the first place.
 */
public class UpdateControlMessage extends ControlMessage {

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
     * Location of the middleware instance.
     */
    private Middleware middleware;

    /**
     * List of endpoint details exposed by the middleware at that middleware.
     */
    private List<EndpointDetails> details;


    /**
     * Instantiate a new immutable UPDATE control message for the given middleware.
     */
    public UpdateControlMessage(@JsonProperty("middleware") Middleware middleware,
                                @JsonProperty("details") List<EndpointDetails> details) {
        this.middleware = middleware;
        this.details = new ArrayList<>(details);
    }

    /**
     * Update the entry in the RDC state, and respond with acknowledgement.
     *
     * @param service A reference to the RDC service receiving the message.
     *
     * @return an acknowledgement response.
     */
    @Override
    public Response handle(Service service) {
        if (!(service instanceof RDCService)) {
            // UPDATE can only be handled by an RDC instance.
            return null;
        }

        RDCService rdc = (RDCService) service;
        rdc.update(middleware, details);

        return Response.getInstance();
    }

    @Override
    public UpdateControlMessage.Response getResponse(RequestStream stream) {
        return (UpdateControlMessage.Response) super.getResponse(stream);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        UpdateControlMessage other = (UpdateControlMessage) obj;

        return Objects.equals(middleware, other.middleware);
    }
}
