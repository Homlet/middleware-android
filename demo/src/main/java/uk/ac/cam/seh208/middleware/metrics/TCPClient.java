package uk.ac.cam.seh208.middleware.metrics;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;

import uk.ac.cam.seh208.middleware.metrics.exception.IncompleteMetricsException;


public class TCPClient implements MetricsClient {

    private class ListenerThread extends Thread {

        private long[] timeRecv;

        private int length;

        volatile boolean closed = false;


        ListenerThread(long[] timeRecv, int length) {
            this.timeRecv = timeRecv;
            this.length = length;
        }

        @Override
        public void run() {
            Log.d(getTag(), "Listener thread running.");

            try {
                // Log the time at which each message was received.
                while (!closed) {
                    String msg = readMsg();
                    if (msg == null) {
                        break;
                    }

                    int seq = Integer.parseInt(msg.split("[\\[,]")[1].trim());
                    timeRecv[seq] = System.nanoTime();
                }
            } catch (InterruptedIOException ignored) {
                // Pass through.
            } catch (IOException e) {
                Log.e(getTag(), "Fatal error in listener thread", e);
            } finally {
                Log.d(getTag(), "Listener thread stopped.");
            }
        }

        private String readMsg() throws IOException {
            InputStream is = client.getInputStream();
            byte[] buffer = new byte[length];
            int remaining = length;

            while (!closed && remaining > 0) {
                int n = is.read(buffer, length - remaining, remaining);
                if (n < 0) {
                    return null;
                }

                remaining -= n;
            }

            if (closed) {
                return null;
            }

            return new String(buffer);
        }
    }


    private Socket client;

    private ListenerThread listenerThread;

    private final String host;


    public TCPClient(String host) {
        this.host = host;
    }

    private void connect() throws IOException {
        // Create and connect the client socket.
        client = new Socket(host, TCPServer.PORT);
    }

    private Metrics run(int messages, int length) throws InterruptedException, IOException {
        if (length < MetricsClient.minLength(messages)) {
            throw new IllegalArgumentException("The minimum length for sending " + messages +
                    "metrics messages is " + MetricsClient.minLength(messages) + " chars due " +
                    "to formatting overheads.");
        }

        Log.i(getTag(), "Running metrics (" + messages + "messages, length " + length + "chars)");

//        client.setSendBufferSize(1024);
//        client.setReceiveBufferSize(1024);

        // Track the send and receive timestamps for each message (in nanoseconds).
        long[] timeSend = new long[messages];
        long[] timeRecv = new long[messages];

        // Start the message listener thread.
        listenerThread = new ListenerThread(timeRecv, length);
        listenerThread.start();

        // Wait for the server to accept the listener connection.
        Thread.sleep(100);

        // Send the metrics messages.
        sendMessages(messages, length, timeSend);

        // Wait for the last message to arrive.
        Thread.sleep(2000);

        // Close and join the listener thread.
        listenerThread.closed = true;
        listenerThread.interrupt();
        listenerThread.join();

        // Process the timing data to produce a metrics object.
        return MetricsClient.process(timeSend, timeRecv, length);
    }

    private void sendMessages(int messages, int length, long[] timeSend) throws IOException {
        // TODO: D.R.Y.
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

        OutputStream os = client.getOutputStream();

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
            os.write(msgBuilder.toString().getBytes());
        }
    }

    private void disconnect() throws IOException {
        // Close the client socket.
        client.close();

        // Drop references.
        listenerThread = null;
        client = null;
    }

    @Override
    public Metrics runMetrics(int messages, int length) throws IncompleteMetricsException {
        // Connect to the metrics server.
        try {
            connect();
        } catch (IOException e) {
            throw new IncompleteMetricsException();
        }

        // Run the metrics, keeping the result.
        Metrics metrics;
        try {
            metrics = run(messages, length);
        } catch (InterruptedException e) {
            return null;
        } catch (IOException e) {
            throw new IncompleteMetricsException();
        }

        // Disconnect from the metrics server.
        try {
            disconnect();
        } catch (IOException e) {
            Log.w(getTag(), "Error disconnecting from metrics server.");
        }

        return metrics;
    }

    @Override
    public String getName() {
        return "tcp";
    }

    private static String getTag() {
        return "METRICS_CLIENT_TCP";
    }
}
