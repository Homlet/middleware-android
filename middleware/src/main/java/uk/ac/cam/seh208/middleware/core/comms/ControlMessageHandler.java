package uk.ac.cam.seh208.middleware.core.comms;

import android.util.Log;

import java.io.IOException;

import uk.ac.cam.seh208.middleware.common.JSONSerializable;
import uk.ac.cam.seh208.middleware.core.MiddlewareService;
import uk.ac.cam.seh208.middleware.core.network.RequestHandler;


/**
 * Request handler implementation for responding to middleware
 * control messages.
 */
public class ControlMessageHandler implements RequestHandler {

    /**
     * Reference to the owning service.
     */
    private MiddlewareService service;


    /**
     * Instantiate a new handler referencing a parent service.
     */
    public ControlMessageHandler(MiddlewareService service) {
        this.service = service;
    }

    /**
     * Invoked by the responder when an incoming control message is received.
     *
     * Decode the type of control message, and then delegate to its handler
     * implementation to generate a response.
     *
     * In the case of an error, null is returned and sent to the peer.
     *
     * @return the response to the control message.
     */
    @Override
    public String respond(String request) {
        try {
            // Parse the control message.
            ControlMessage message = JSONSerializable.fromJSON(request, ControlMessage.class);

            // Defer to the control message handler implementation.
            ControlMessage.Response response = message.handle(service);
            return response.toJSON();
        } catch (IOException e) {
            Log.e(getTag(), "Error parsing control message: \"" + request + "\"");
            return null;
        }
    }

    private static String getTag() {
        return "MIDDLEWARE_HANDLER";
    }
}
