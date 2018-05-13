package uk.ac.cam.seh208.middleware.metrics;

import android.util.Log;

import java8.util.Lists;
import uk.ac.cam.seh208.middleware.api.Endpoint;
import uk.ac.cam.seh208.middleware.api.Middleware;
import uk.ac.cam.seh208.middleware.api.exception.*;


public class MiddlewareServer implements MetricsServer {

    private static String ENDPOINT_SINK = "metrics_server_sink";

    private static String ENDPOINT_SOURCE = "metrics_server_source";


    private Middleware middleware;

    private volatile boolean started;


    public MiddlewareServer(Middleware middleware) {
        this.middleware = middleware;
        started = false;
    }

    public synchronized void start() {
        if (started) {
            return;
        }

        try {
            // Create the metrics endpoints.
            middleware.createSource(
                    ENDPOINT_SOURCE,
                    "The source endpoint that sends message " +
                            "receipts back to the metrics test.",
                    "{\"type\":\"array\", \"items\":[{\"type\":\"number\"}, " +
                            "{\"type\":\"string\"}]}",
                    Lists.of("metrics", "source", "receipt", "server"),
                    true,
                    false);
            middleware.createSink(
                    ENDPOINT_SINK,
                    "The sink endpoint that accepts messages " +
                            "from the metrics test.",
                    "{\"type\":\"array\", \"items\":[{\"type\":\"number\"}, " +
                            "{\"type\":\"string\"}]}",
                    Lists.of("metrics", "sink", "input", "server"),
                    true,
                    false);

            // Configure the source to send receipts back
            // for all incoming messages.
            Endpoint sink = middleware.getEndpoint(ENDPOINT_SINK);
            sink.registerListener(this::onMessage);

            started = true;
        } catch (MiddlewareDisconnectedException e) {
            Log.e(getTag(), "Couldn't start server due to " +
                    "middleware disconnection.");
        }
    }

    public synchronized void stop() {
        if (!started) {
            return;
        }

        try {
            // Destroy the endpoints (if they exists).
            middleware.destroyEndpoint(ENDPOINT_SOURCE);
            middleware.destroyEndpoint(ENDPOINT_SINK);

            started = false;
        } catch (MiddlewareDisconnectedException e) {
            Log.w(getTag(), "Middleware disconnected " +
                    "while stopping the server.");
        }
    }

    public boolean isStarted() {
        return started;
    }

    private void onMessage(String message) {
        try {
            // Send a receipt back over the source endpoint.
            Endpoint source = middleware.getEndpoint(ENDPOINT_SOURCE);
            source.send(message);
        } catch (MiddlewareDisconnectedException e) {
            Log.e(getTag(), "Failed to send receipt due to " +
                    "middleware disconnection.");
        }
    }

    private static String getTag() {
        return "METRICS_SERVER_MW";
    }
}
