package uk.ac.cam.seh208.middleware.core.comms;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.common.RemoteEndpointDetails;


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
            this.details = new ArrayList<>();
            this.details.addAll(details);
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
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private RemoteEndpointDetails localEndpoint;

    /**
     * Query used to filter endpoints on the remote host.
     */
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private Query query;


    /**
     * Instantiate a new immutable OPEN_CHANNELS control message with the given query.
     */
    public OpenChannelsControlMessage(
            @JsonProperty("localEndpoint") RemoteEndpointDetails localEndpoint,
            @JsonProperty("query") Query query) {
        this.localEndpoint = localEndpoint;
        this.query = query;
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

        return (Objects.equals(localEndpoint, other.localEndpoint)
             && Objects.equals(query, other.query));
    }
}
