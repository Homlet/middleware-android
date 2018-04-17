package uk.ac.cam.seh208.middleware.core.control;

import android.app.Service;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.IOException;

import uk.ac.cam.seh208.middleware.common.JSONSerializable;
import uk.ac.cam.seh208.middleware.core.comms.RequestStream;


/**
 * Immutable data object representing a serializable control message.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "tag"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OpenChannelsControlMessage.class, name = "OPEN_CHANNELS"),
        @JsonSubTypes.Type(value = CloseChannelControlMessage.class, name = "CLOSE_CHANNEL"),
        @JsonSubTypes.Type(value = QueryControlMessage.class, name = "QUERY"),
        @JsonSubTypes.Type(value = UpdateControlMessage.class, name = "UPDATE"),
        @JsonSubTypes.Type(value = RemoveControlMessage.class, name = "REMOVE"),
        @JsonSubTypes.Type(value = MiddlewareCommandControlMessage.class, name = "MW_COMMAND"),
        @JsonSubTypes.Type(value = EndpointCommandControlMessage.class, name = "EP_COMMAND")
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
            @JsonSubTypes.Type(value = OpenChannelsControlMessage.Response.class,
                               name = "OPEN_CHANNELS.R"),
            @JsonSubTypes.Type(value = CloseChannelControlMessage.Response.class,
                               name = "CLOSE_CHANNEL.R"),
            @JsonSubTypes.Type(value = QueryControlMessage.Response.class,
                               name = "QUERY.R"),
            @JsonSubTypes.Type(value = UpdateControlMessage.Response.class,
                               name = "UPDATE.R"),
            @JsonSubTypes.Type(value = RemoveControlMessage.Response.class,
                               name = "REMOVE.R"),
            @JsonSubTypes.Type(value = MiddlewareCommandControlMessage.Response.class,
                               name = "MW_COMMAND.R"),
            @JsonSubTypes.Type(value = EndpointCommandControlMessage.Response.class,
                               name = "EP_COMMAND.R")
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
     * @param service A reference to the middleware or RDC service receiving the message.
     *
     * @return a Response object representing the result.
     */
    public abstract Response handle(Service service);
}
