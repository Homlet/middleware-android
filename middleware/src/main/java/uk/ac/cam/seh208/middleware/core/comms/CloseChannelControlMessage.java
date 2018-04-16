package uk.ac.cam.seh208.middleware.core.comms;

import android.app.Service;

import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.cam.seh208.middleware.core.MiddlewareService;
import uk.ac.cam.seh208.middleware.core.network.RequestStream;


/**
 * Control message sent to a remote middleware host to indicate that a channel
 * should be immediately torn down gracefully.
 */
public class CloseChannelControlMessage extends ControlMessage {

    /**
     * The response indicates whether the channel was closed (true), or whether the
     * channel identifier was not recognised (false).
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
     * Unique identifier of the channel that should be closed.
     */
    private long channelId;


    /**
     * Instantiate a new immutable CLOSE_CHANNEL control message for the given channel.
     */
    public CloseChannelControlMessage(@JsonProperty("channelId") long channelId) {
        this.channelId = channelId;
    }

    /**
     * Close the channel via the service, and encapsulate the result in a response object.
     *
     * @param service A reference to the middleware service receiving the message.
     *
     * @return a response containing the details of success.
     */
    @Override
    public Response handle(Service service) {
        if (!(service instanceof MiddlewareService)) {
            // Close channel can only be handled by a middleware.
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
        CloseChannelControlMessage other = (CloseChannelControlMessage) obj;

        return channelId == other.channelId;
    }
}
