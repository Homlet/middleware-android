package uk.ac.cam.seh208.middleware.core.control;

import android.app.Service;

import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.cam.seh208.middleware.core.MiddlewareService;
import uk.ac.cam.seh208.middleware.core.comms.RequestStream;


/**
 * Control message sent to a remote middleware host to indicate that a link
 * should be immediately torn down gracefully.
 */
public class CloseLinkControlMessage extends ControlMessage {

    /**
     * The response indicates whether the link was closed (true), or whether the
     * link identifier was not recognised (false).
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
     * Unique identifier of the link that should be closed.
     */
    private long linkId;


    /**
     * Instantiate a new immutable CLOSE-LINK control message for the given link.
     */
    public CloseLinkControlMessage(@JsonProperty("linkId") long linkId) {
        this.linkId = linkId;
    }

    /**
     * Close the link via the service, and encapsulate the result in a response object.
     *
     * @param service A reference to the middleware service receiving the message.
     *
     * @return a response containing the details of success.
     */
    @Override
    public Response handle(Service service) {
        if (!(service instanceof MiddlewareService)) {
            // Close link can only be handled by a middleware.
            return null;
        }

        // TODO: implement.
        return null;
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
        CloseLinkControlMessage other = (CloseLinkControlMessage) obj;

        return linkId == other.linkId;
    }
}
