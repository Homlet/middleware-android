package uk.ac.cam.seh208.middleware.demo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import java8.util.Lists;
import uk.ac.cam.seh208.middleware.api.*;
import uk.ac.cam.seh208.middleware.common.Persistence;
import uk.ac.cam.seh208.middleware.common.Query;


public class ExampleSinkActivity extends Activity {

    /**
     * Name of the endpoint this activity creates.
     */
    private static final String EP_NAME = "example_sink";


    /**
     * Instance of the middleware interface bound
     * to this activity.
     */
    private Middleware middleware;

    /**
     * Instance of the endpoint interface bound
     * to this activity.
     */
    private Endpoint sink;

    /**
     * Token to the created mapping.
     */
    private long mapping = -1;

    /**
     * Token to the callback for receiving messages.
     */
    private MessageListenerToken listener = null;


    /**
     * Called on creation of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Instantiate the middleware and endpoint interfaces.
        middleware = new Middleware(this);
        sink = middleware.getEndpoint(EP_NAME);
    }

    /**
     * Called when the activity starts running, and the UI is presented.
     */
    @Override
    protected void onStart() {
        super.onStart();

        // Connect to the middleware service.
        middleware.bind(this::onMiddlewareBind);
    }

    /**
     * Called when the middleware service has started and been bound to
     * the activity. The middleware is now ready for API calls to be made.
     */
    protected void onMiddlewareBind() {
        Log.i("EXAMPLE", "Middleware bound successfully.");

        try {
            // Set the RDC address as the local loopback address.
            middleware.setRDCAddress("zmq://127.0.0.1:4854");

            // Create a sink endpoint which accepts strings.
            if (!middleware.doesEndpointExist(EP_NAME)) {
                middleware.createSink(
                        EP_NAME,                                   // name
                        "A test endpoint which accepts strings.",  // desc
                        "{\"type\": \"string\"}",                // schema
                        Lists.of("test", "sink", "string"),        // tags
                        false,                                  // exposed
                        false                                 // forceable
                );
            }

            // Register a simple listener with the endpoint.
            listener = sink.registerListener(message ->
                    Log.i("EXAMPLE", "Message: " + message));

            // Map the endpoint to peers.
            mapping = sink.map(new Query.Builder()
                    .includeTag("test")
                    .includeTag("string")
                    .build(), Persistence.NONE);
        } catch (MiddlewareDisconnectedException e) {
            Log.e("EXAMPLE", "Middleware disconnected " +
                    "while configuring.");
        }
    }

    /**
     * Called when the activity stops running. This may
     * include the activity moving into the background.
     */
    @Override
    protected void onStop() {
        super.onStop();

        try {
            // Close the established mapping.
            if (mapping != -1) {
                sink.unmap(mapping);
                mapping = -1;
            }

            // Remove the message listener.
            if (listener != null) {
                sink.unregisterListener(listener);
                listener = null;
            }
        } catch (MiddlewareDisconnectedException e) {
            Log.e("EXAMPLE", "Middleware disconnected " +
                             "while stopping.");
            finish();
        }

        // Disconnect from the middleware service.
        middleware.unbind();
    }
}
