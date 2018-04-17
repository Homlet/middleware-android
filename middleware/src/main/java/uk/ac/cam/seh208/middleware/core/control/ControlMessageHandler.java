package uk.ac.cam.seh208.middleware.core.control;

import android.app.Service;
import android.util.Log;

import java.io.IOException;

import uk.ac.cam.seh208.middleware.common.JSONSerializable;
import uk.ac.cam.seh208.middleware.core.comms.RequestHandler;


/**
 * Request handler implementation for responding to middleware
 * control messages.
 */
public class ControlMessageHandler implements RequestHandler {

    /**
     * Reference to the owning service.
     */
    private Service service;


    /**
     * Instantiate a new handler referencing a parent service.
     */
    public ControlMessageHandler(Service service) {
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
            Log.e(getTag(), "Error handling control message: \"" + request + "\"");
            return null;
        }
    }

    private static String getTag() {
        return "HANDLER";
    }
}
