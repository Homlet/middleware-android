package uk.ac.cam.seh208.middleware.core.comms;

import android.app.Service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import uk.ac.cam.seh208.middleware.common.EndpointCommand;
import uk.ac.cam.seh208.middleware.core.MiddlewareService;
import uk.ac.cam.seh208.middleware.core.network.RequestStream;


/**
 * Control message sent to another middleware instance to run a command on it.
 */
public class EndpointCommandControlMessage extends ControlMessage {

    /**
     * The response indicates whether the command was successfully run.
     */
    public static class Response extends ControlMessage.Response {

        private boolean success;


        public Response(@JsonProperty("success") boolean success) {
            this.success = success;
        }

        public boolean getSuccess() {
            return success;
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

            return success == other.success;
        }
    }


    /**
     * Name of the endpoint on which to run the command.
     */
    private String name;

    /**
     * Data representation of the command.
     */
    private EndpointCommand command;


    /**
     * Instantiate a new immutable EP_COMMAND control message for the given command.
     */
    public EndpointCommandControlMessage(
            @JsonProperty("name") String name,
            @JsonProperty("command") EndpointCommand command) {
        this.name = name;
        this.command = command;
    }

    /**
     * Force the command to execute on the endpoint, and respond with success or failure.
     *
     * @param service A reference to the middleware service receiving the message.
     *
     * @return a response indicating whether the command ran successfully.
     */
    @Override
    public Response handle(Service service) {
        if (!(service instanceof MiddlewareService)) {
            // EP_COMMAND can only be handled by a middleware instance.
            return null;
        }

        // Execute the command on the endpoint.
        MiddlewareService middleware = (MiddlewareService) service;
        Endpoint endpoint = middleware.getEndpointSet().getEndpointByName(name);
        if (endpoint == null) {
            return new Response(false);
        }
        return new Response(endpoint.execute(command));
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
        EndpointCommandControlMessage other = (EndpointCommandControlMessage) obj;

        return (Objects.equals(name, other.name) &&
                Objects.equals(command, other.command));
    }
}
