package uk.ac.cam.seh208.middleware.metrics;

import android.util.Log;

import java8.util.Lists;
import uk.ac.cam.seh208.middleware.api.Endpoint;
import uk.ac.cam.seh208.middleware.api.MessageListener;
import uk.ac.cam.seh208.middleware.api.MessageListenerToken;
import uk.ac.cam.seh208.middleware.api.Middleware;
import uk.ac.cam.seh208.middleware.api.exception.MiddlewareDisconnectedException;
import uk.ac.cam.seh208.middleware.common.Persistence;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.metrics.exception.IncompleteMetricsException;


public class MiddlewareClient implements MetricsClient {

    private static String ENDPOINT_SINK = "metrics_client_sink";

    private static String ENDPOINT_SOURCE = "metrics_client_source";


    private Middleware middleware;

    private Endpoint source;

    private Endpoint sink;


    public MiddlewareClient(Middleware middleware) {
        this.middleware = middleware;
        source = middleware.getEndpoint(ENDPOINT_SOURCE);
        sink = middleware.getEndpoint(ENDPOINT_SINK);
    }

    private void connect() throws MiddlewareDisconnectedException {
        Log.i(getTag(), "Connecting to the middleware metrics server.");

        // Create the metrics client endpoints.
        middleware.createSource(
                ENDPOINT_SOURCE,
                "A source endpoint which sends messages to the metrics server.",
                "{\"type\":\"array\", \"items\":[{\"type\":\"number\"}, {\"type\":\"string\"}]}",
                Lists.of("metrics", "source", "client"),
                false,
                false);
        middleware.createSink(
                ENDPOINT_SINK,
                "A sink endpoint which accepts message receipts from the metrics server.",
                "{\"type\":\"array\", \"items\":[{\"type\":\"number\"}, {\"type\":\"string\"}]}",
                Lists.of("metrics", "sink", "receipt", "client"),
                false,
                false);

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
    }

    private Metrics run(int messages, int length) throws MiddlewareDisconnectedException {
        if (length < MetricsClient.minLength(messages)) {
            throw new IllegalArgumentException("The minimum length for sending " + messages +
                    "metrics messages is " + MetricsClient.minLength(messages) + " chars due " +
                    "to formatting overheads.");
        }

        Log.i(getTag(), "Running metrics (" + messages + "messages, length " + length + "chars)");

        // Store the in and out times (in relative nanoseconds) of every message.
        long[] timeSend = new long[messages];
        long[] timeRecv = new long[messages];

        // Register the message listener.
        MessageListenerToken token = sink.registerListener(makeListener(timeRecv));

        // Send the metrics messages.
        sendMessages(messages, length, timeSend);

        // Wait for the last message to arrive.
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            return null;
        }

        // Unregister the message listener now all the messages have been received (or dropped).
        sink.unregisterListener(token);

        // Process the timing data to produce a metrics object.
        return MetricsClient.process(timeSend, timeRecv, length);
    }

    private MessageListener makeListener(long[] timeRecv) {
        return msg -> {
            // Record the receive time for the message sequence number.
            int seq = Integer.parseInt(msg.split("[\\[,]")[1].trim());
            timeRecv[seq] = System.nanoTime();
        };
    }

    private void sendMessages(int messages, int length, long[] timeSend) throws MiddlewareDisconnectedException {
        // Work out and insert the amount of string padding required for the message format.
        int padding = length - MetricsClient.minLength(messages);
        String format = "[,\"";
        for (int i = 0; i < padding; i++) {
            //noinspection StringConcatenationInLoop
            format += "P";
        }
        format += "\"]";

        // Create a string builder to use as a prototype for efficient message building.
        StringBuilder msgBuilderPrototype = new StringBuilder(format);

        // Send all messages in turn.
        for (int seq = 0; seq < messages; seq++) {
            // Create a new string builder for the message, and insert the sequence number.
            StringBuilder msgBuilder = new StringBuilder(msgBuilderPrototype);
            msgBuilder.insert(1, seq);

            // Space pad the sequence number up to the correct message length.
            int zeroes = length - msgBuilder.length();
            for (int j = 0; j < zeroes; j++) {
                msgBuilder.insert(1, " ");
            }

            timeSend[seq] = System.nanoTime();
            source.send(msgBuilder.toString());
        }
    }

    private void disconnect() throws MiddlewareDisconnectedException {
        Log.i(getTag(), "Disconnecting from the middleware metrics server.");

        // Close the mappings and destroy the endpoints.
        source.unmapAll();
        sink.unmapAll();
        middleware.destroyEndpoint(ENDPOINT_SOURCE);
        middleware.destroyEndpoint(ENDPOINT_SINK);
    }

    @Override
    public Metrics runMetrics(int messages, int length) throws IncompleteMetricsException {
        try {
            // Connect to the metrics server.
            connect();
        } catch (MiddlewareDisconnectedException e) {
            Log.e(getTag(), "Couldn't map to metrics server due to middleware disconnection.");
            throw new IncompleteMetricsException();
        }

        // Run the metrics, keeping the result.
        Metrics metrics;
        try {
            metrics = run(messages, length);
        } catch (MiddlewareDisconnectedException e) {
            Log.e(getTag(), "Failed to run metrics due to middleware disconnection.");
            throw new IncompleteMetricsException();
        }

        try {
            // Disconnect from the metrics server.
            disconnect();
        } catch (MiddlewareDisconnectedException e) {
            Log.w(getTag(), "Middleware disconnected while closing metrics client.");
        }

        return metrics;
    }

    @Override
    public String getName() {
        return "mw";
    }

    private static String getTag() {
        return "METRICS_CLIENT_MW";
    }
}
