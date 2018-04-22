package uk.ac.cam.seh208.middleware.metrics;

import android.util.Log;

import java8.util.Lists;
import uk.ac.cam.seh208.middleware.api.Endpoint;
import uk.ac.cam.seh208.middleware.api.MessageListenerToken;
import uk.ac.cam.seh208.middleware.api.Middleware;
import uk.ac.cam.seh208.middleware.api.exception.MiddlewareDisconnectedException;
import uk.ac.cam.seh208.middleware.common.Persistence;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.metrics.exception.IncompleteMetricsException;


// TODO: use principles from ZeroMQ guide.
public class MiddlewareClient implements MetricsClient {

    @SuppressWarnings("FieldCanBeLocal")
    private static String ENDPOINT_SINK = "metrics_client_sink";

    @SuppressWarnings("FieldCanBeLocal")
    private static String ENDPOINT_SOURCE = "metrics_client_source";


    private Middleware middleware;

    private float latency;

    private int latencyCount;

    private float throughput;

    private int throughputCount;

    private int length;

    private boolean connected;


    public MiddlewareClient(Middleware middleware) {
        this.middleware = middleware;
        latency = -1;
        latencyCount = 0;
        throughput = -1;
        throughputCount = 0;
        length = -1;
        connected = false;
    }

    public void connect() {
        if (connected) {
            return;
        }

        try {
            // Create the metrics client endpoints.
            middleware.createSource(
                    ENDPOINT_SOURCE,
                    "A source endpoint which sends messages to the metrics server.",
                    "{\"type\": \"number\"}",
                    Lists.of("metrics", "source", "client"),
                    false,
                    false);
            middleware.createSink(
                    ENDPOINT_SINK,
                    "A sink endpoint which accepts message receipts from the metrics server.",
                    "{\"type\": \"number\"}",
                    Lists.of("metrics", "sink", "receipt", "client"),
                    false,
                    false);
            Endpoint source = middleware.getEndpoint(ENDPOINT_SOURCE);
            Endpoint sink = middleware.getEndpoint(ENDPOINT_SINK);

            // Map the endpoints to the server.
            // TODO: map the sink to the same peer using mapTo.
            source.map(
                    new Query.Builder()
                            .setMatches(1)
                            .includeTag("metrics")
                            .includeTag("server")
                            .build(),
                    Persistence.NONE);
            sink.map(
                    new Query.Builder()
                            .setMatches(1)
                            .includeTag("metrics")
                            .includeTag("server")
                            .build(),
                    Persistence.NONE);

            connected = true;
        } catch (MiddlewareDisconnectedException e) {
            Log.e(getTag(), "Couldn't map to metrics server due to middleware disconnection.");
        }
    }

    public void disconnect() {
        Endpoint source = middleware.getEndpoint(ENDPOINT_SOURCE);
        Endpoint sink = middleware.getEndpoint(ENDPOINT_SINK);

        try {
            // Close the mappings and destroy the endpoints.
            source.unmapAll();
            sink.unmapAll();
            middleware.destroyEndpoint(ENDPOINT_SOURCE);
            middleware.destroyEndpoint(ENDPOINT_SINK);

            connected = false;
        } catch (MiddlewareDisconnectedException e) {
            Log.w(getTag(), "Middleware disconnected while closing metrics client.");
        }
    }

    public void runLatency(int messages, int delayMillis) {
        if (!connected) {
            // If we are not connected to the metrics server, we cannot run metrics.
            Log.w(getTag(), "Attempted to run metrics while not connected to server.");
            return;
        }

        Endpoint source = middleware.getEndpoint(ENDPOINT_SOURCE);
        Endpoint sink = middleware.getEndpoint(ENDPOINT_SINK);

        try {
            // Create a message listener for measuring message parameters.
            int[] seen = new int[1];
            MessageListenerToken token = sink.registerListener(message -> {
                // Get the current timestamp.
                long now = System.nanoTime();

                // Extract the timestamp from the message.
                long then = Long.parseLong(message);

                float measurement = (now - then) / 1000;
                Log.v(getTag(), "Latency: " + measurement + "us");
                latency *= latencyCount / (latencyCount + 1);
                latency += measurement * (1.0f / (latencyCount + 1));

                // Increment the seen message counter.
                seen[0]++;
            });

            for (int i = 0; i < messages; i++) {
                Thread.sleep(delayMillis);

                // Send the current time.
                source.send(String.valueOf(System.nanoTime()));
            }

            // Wait for receipt messages to arrive.
            Thread.sleep(50);

            // Unregister the message listener from the sink for future use.
            sink.unregisterListener(token);

            Log.i(getTag(), "Gathered middleware latency metrics (" +
                    (messages - seen[0]) + " dropped messages)");
        } catch (MiddlewareDisconnectedException e) {
            Log.e(getTag(), "Couldn't run latency metrics due to middleware disconnection.");
        } catch (InterruptedException e) {
            Log.w(getTag(), "Latency test was interrupted before completion.");
        }
    }

    @Override
    public void runThroughput(int messages) {
        if (!connected) {
            // If we are not connected to the metrics server, we cannot run metrics.
            Log.w(getTag(), "Attempted to run metrics while not connected to server.");
            return;
        }

        // TODO: implement.
    }

    @Override
    public Metrics getMetrics() throws IncompleteMetricsException {
//        if (latency < 0 || throughput < 0 || length < 0) {
//            // If the running averages have not been initialised, the metrics are incomplete.
//            throw new IncompleteMetricsException();
//        }

        // Construct a new immutable metrics object with the stored running averages.
        return new Metrics(latency, throughput, length);
    }

    private static String getTag() {
        return "METRICS_CLIENT_MW";
    }
}
