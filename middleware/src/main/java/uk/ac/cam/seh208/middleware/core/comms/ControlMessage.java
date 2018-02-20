package uk.ac.cam.seh208.middleware.core.comms;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.IOException;

import uk.ac.cam.seh208.middleware.common.JSONSerializable;
import uk.ac.cam.seh208.middleware.core.MiddlewareService;
import uk.ac.cam.seh208.middleware.core.network.RequestStream;


/**
 * Immutable data object representing a serializable control message.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "tag"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OpenChannelsControlMessage.class, name = "m0")
        // TODO: implement the rest.
})
public abstract class ControlMessage implements JSONSerializable {

    /**
     * Superclass for response types. These are specified within the relevant control
     * messages.
     */
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "tag"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = OpenChannelsControlMessage.Response.class, name = "r0")
            // TODO: implement the rest.
    })
    public static abstract class Response implements JSONSerializable { }


    /**
     * Send the control message over a given stream, and return the response.
     *
     * @param stream A ready-for-sending request stream to the remote host.
     *
     * @return a reference to a response object.
     */
    public Response getResponse(RequestStream stream) {
        try {
            // Serialise this message over the stream and get the string response.
            String response = stream.request(this.toJSON());

            // If no response was received, return null.
            if (response == null) {
                return null;
            }

            return JSONSerializable.fromJSON(response, Response.class);
        } catch (IOException e) {
            // Return null to indicate a bad response was received.
            return null;
        }
    }

    /**
     * Handle the control message according to its definition.
     *
     * @param service A reference to the middleware service receiving the message.
     *
     * @return a Response object representing the result.
     */
    public abstract Response handle(MiddlewareService service);
}
