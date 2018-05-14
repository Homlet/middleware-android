package uk.ac.cam.seh208.middleware.core.control;

import android.app.Service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.core.RDCService;
import uk.ac.cam.seh208.middleware.core.comms.RequestStream;


/**
 * Control message sent to an RDC to request a list of locations
 * exposing endpoints matching the stored query.
 */
public class QueryControlMessage extends ControlMessage {

    /**
     * The response contains the list of remote endpoints which matched the
     * query, and with which links were established.
     */
    public static class Response extends ControlMessage.Response {

        private List<Middleware> middlewares;


        public Response(@JsonProperty("middlewares") List<Middleware> middlewares) {
            // Copy the passed details list so the internal state of this
            // immutable object cannot be modified.
            this.middlewares = new ArrayList<>(middlewares);
        }

        public List<Middleware> getMiddlewares() {
            return Collections.unmodifiableList(middlewares);
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

            return Objects.equals(
                    new HashSet<>(middlewares),
                    new HashSet<>(other.middlewares));
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
        if (!(service instanceof RDCService)) {
            // REMOVE can only be handled by an RDC instance.
            return null;
        }

        // Interpret the given service as an RDCService.
        RDCService rdc = (RDCService) service;

        // Discover resources and return them wrapped in a response message.
        return new Response(rdc.discover(query));
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
