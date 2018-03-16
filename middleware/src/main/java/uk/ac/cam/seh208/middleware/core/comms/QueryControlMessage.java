package uk.ac.cam.seh208.middleware.core.comms;

import android.app.Service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.core.network.Location;
import uk.ac.cam.seh208.middleware.core.network.RequestStream;


/**
 * Control message sent to an RDC to request a list of locations
 * exposing endpoints matching the stored query.
 */
public class QueryControlMessage extends ControlMessage {

    /**
     * The response contains the list of remote endpoints which matched the
     * query, and with which channels were established.
     */
    public static class Response extends ControlMessage.Response {

        private List<Location> locations;


        public Response(@JsonProperty("locations") List<Location> locations) {
            // Copy the passed details list so the internal state of this
            // immutable object cannot be modified.
            this.locations = new ArrayList<>(locations);
        }

        public List<Location> getLocations() {
            return Collections.unmodifiableList(locations);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            QueryControlMessage.Response other = (QueryControlMessage.Response) obj;

            return Objects.equals(locations, other.locations);  // TODO: reintroduce HashSets.
        }
    }


    /**
     * Query used to filter endpoints on the RDC.
     */
    private Query query;


    /**
     * Instantiate a new immutable QUERY control message with the given query.
     */
    public QueryControlMessage(@JsonProperty("query") Query query) {
        this.query = query;
    }

    /**
     * Query via the RDC, and encapsulate the result in a response object.
     *
     * @param service A reference to the RDC service receiving the message.
     *
     * @return a response containing the locations matching the query.
     */
    @Override
    public Response handle(Service service) {
        // TODO: implement.
        return null;
    }

    @Override
    public QueryControlMessage.Response getResponse(RequestStream stream) {
        return (QueryControlMessage.Response) super.getResponse(stream);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        QueryControlMessage other = (QueryControlMessage) obj;

        return Objects.equals(query, other.query);
    }
}
