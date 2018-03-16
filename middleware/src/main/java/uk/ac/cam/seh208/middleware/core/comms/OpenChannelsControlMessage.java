package uk.ac.cam.seh208.middleware.core.comms;

import android.app.Service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.core.MiddlewareService;
import uk.ac.cam.seh208.middleware.core.network.RequestStream;


/**
 * Control message sent to a remote host to indicate that a number of channels
 * should immediately be established with endpoints that match the sent query.
 */
public class OpenChannelsControlMessage extends ControlMessage {

    /**
     * The response contains the list of remote endpoints which matched the
     * query, and with which channels were established.
     */
    public static class Response extends ControlMessage.Response {

        private List<RemoteEndpointDetails> details;


        public Response(@JsonProperty("details") List<RemoteEndpointDetails> details) {
            // Copy the passed details list so the internal state of this
            // immutable object cannot be modified.
            this.details = new ArrayList<>(details);
        }

        public List<RemoteEndpointDetails> getDetails() {
            return Collections.unmodifiableList(details);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Response other = (Response) obj;

            return Objects.equals(details, other.details);  // TODO: reintroduce HashSets.
        }
    }


    /**
     * Remote view of the local endpoint with which channels should be established.
     */
    private RemoteEndpointDetails initiatorEndpoint;

    /**
     * Query used to filter endpoints on the remote host.
     */
    private Query query;


    /**
     * Instantiate a new immutable OPEN_CHANNELS control message with the given query.
     */
    public OpenChannelsControlMessage(
            @JsonProperty("initiatorEndpoint") RemoteEndpointDetails initiatorEndpoint,
            @JsonProperty("query") Query query) {
        this.initiatorEndpoint = initiatorEndpoint;
        this.query = query;
    }

    /**
     * Open channels via the service, and encapsulate the result in a response object.
     *
     * @param service A reference to the middleware service receiving the message.
     *
     * @return a response containing the details of the opened channels.
     */
    @Override
    public Response handle(Service service) {
        if (!(service instanceof MiddlewareService)) {
            // Open channels can only be handled by a middleware.
            return null;
        }

        // Open channels according to the stored query.
        MiddlewareService middleware = (MiddlewareService) service;
        return new Response(middleware.openChannels(query, initiatorEndpoint));
    }

    @Override
    public Response getResponse(RequestStream stream) {
        return (Response) super.getResponse(stream);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        OpenChannelsControlMessage other = (OpenChannelsControlMessage) obj;

        return (Objects.equals(initiatorEndpoint, other.initiatorEndpoint)
             && Objects.equals(query, other.query));
    }
}
