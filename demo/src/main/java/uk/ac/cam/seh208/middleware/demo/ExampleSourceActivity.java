package uk.ac.cam.seh208.middleware.demo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java8.util.Lists;
import uk.ac.cam.seh208.middleware.api.Endpoint;
import uk.ac.cam.seh208.middleware.api.Middleware;
import uk.ac.cam.seh208.middleware.api.RDC;
import uk.ac.cam.seh208.middleware.api.exception.MiddlewareDisconnectedException;


public class ExampleSourceActivity extends Activity {

    /**
     * Name of the endpoint this activity creates.
     */
    private static final String EP_NAME = "example_source";


    /**
     * Instance of the middleware interface bound to this activity.
     */
    private Middleware middleware;

    /**
     * Instance of the endpoint interface bound to this activity.
     */
    private Endpoint source;

    /**
     * Instance of the handler which runs sending code.
     */
    private Handler handler;

    /**
     * Callback for the handler to send messages.
     */
    private Runnable sender = new Runnable() {
        @Override
        public void run() {
            try {
                handler.postDelayed(sender, 1000);
                source.send("\"test\"");
            } catch (MiddlewareDisconnectedException e) {
                Log.e("EXAMPLE", "Failed to send message over source endpoint.");
            }
        }
    };


    /**
     * Called on creation of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Instantiate the middleware and endpoint interfaces.
        middleware = new Middleware(this);
        source = middleware.getEndpoint(EP_NAME);

        // Instantiate the handler on the UI looper.
        handler = new Handler();

        // Start the RDC on the local device.
        RDC.start(this);
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
     * Called when the activity stops running. This may include the
     * activity moving into the background.
     */
    @Override
    protected void onStop() {
        super.onStop();

        // Prevent further sends from being attempted in the handler.
        handler.removeCallbacks(sender);

        // Disconnect from the middleware service.
        middleware.unbind();
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

            // Create a source endpoint which emits strings.
            if (!middleware.doesEndpointExist(EP_NAME)) {
                middleware.createSource(
                        EP_NAME,                                        // name
                        "A test source endpoint which emits strings.",  // description
                        "{\"type\": \"string\"}",                       // schema
                        Lists.of("test", "source", "string"),           // tags
                        true,                                           // exposed
                        true                                            // forceable
                );
            }

            // Emit data via the source endpoint.
            handler.post(sender);
        } catch (MiddlewareDisconnectedException e) {
            Log.e("EXAMPLE", "Middleware disconnected while configuring.");
            finish();
        }
    }
}
