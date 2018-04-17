package uk.ac.cam.seh208.middleware.core.control;

import android.app.Service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import uk.ac.cam.seh208.middleware.common.MiddlewareCommand;
import uk.ac.cam.seh208.middleware.core.MiddlewareService;
import uk.ac.cam.seh208.middleware.core.comms.RequestStream;


/**
 * Control message sent to another middleware instance to run a command on it.
 */
public class MiddlewareCommandControlMessage extends ControlMessage {

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
     * Data representation of the command.
     */
    private MiddlewareCommand command;


    /**
     * Instantiate a new immutable MW_COMMAND control message for the given command.
     */
    public MiddlewareCommandControlMessage(@JsonProperty("command") MiddlewareCommand command) {
        this.command = command;
    }

    /**
     * Force the command to execute on the service, and respond with success or failure.
     *
     * @param service A reference to the middleware service receiving the message.
     *
     * @return a response indicating whether the command ran successfully.
     */
    @Override
    public Response handle(Service service) {
        if (!(service instanceof MiddlewareService)) {
            // MW_COMMAND can only be handled by a middleware instance.
            return null;
        }

        // Execute the command on the middleware.
        MiddlewareService middleware = (MiddlewareService) service;
        return new Response(middleware.execute(command));
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
        MiddlewareCommandControlMessage other = (MiddlewareCommandControlMessage) obj;

        return (Objects.equals(command, other.command));
    }
}
