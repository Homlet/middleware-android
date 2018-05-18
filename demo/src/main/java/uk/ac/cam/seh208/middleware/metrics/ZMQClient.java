package uk.ac.cam.seh208.middleware.metrics;

import android.util.Log;

import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import static org.zeromq.ZMQ.DEALER;


public class ZMQClient implements MetricsClient {

    private class ListenerThread extends Thread {

        private long[] timeRecv;

        volatile boolean closed = false;


        ListenerThread(long[] timeRecv) {
            this.timeRecv = timeRecv;
        }

        @Override
        public void run() {
            // Create and bind the receipt dealer.
            ZMQ.Socket receiptDealer = context.socket(DEALER);
            receiptDealer.setReceiveTimeOut(200);
            receiptDealer.connect("tcp://" + host + ":" + ZMQServer.PORT_RECEIPT);

            Log.d(getTag(), "Listener thread running.");

            try {
                // Log the time at which each message was received.
                while (!closed) {
                    String msg = receiptDealer.recvStr();
                    if (msg == null) {
                        // recvStr returns null on interruption.
                        continue;
                    }

                    int seq = Integer.parseInt(msg.split("[\\[,]")[1].trim());
                    timeRecv[seq] = System.nanoTime();
                }
            } catch (ZMQException e) {
                if (e.getErrorCode() != ZMQ.Error.ETERM.getCode()) {
                    // This was not thrown due to context termination.
                    Log.e(getTag(), "Fatal error in listener thread", e);
                }
            } finally {
                // Close the receipt socket.
                receiptDealer.close();

                Log.d(getTag(), "Listener thread stopped.");
            }
        }
    }

    private static final int IO_THREADS = 2;


    private ZMQ.Context context;

    private ZMQ.Socket messageDealer;

    private ListenerThread listenerThread;

    private final String host;


    public ZMQClient(String host) {
        this.host = host;
    }

    private void connect() {
        // Open a new ZeroMQ context.
        context = ZMQ.context(IO_THREADS);

        // Create and connect the message dealer.
        messageDealer = context.socket(DEALER);
        messageDealer.setLinger(0);
        messageDealer.setSendTimeOut(200);
        messageDealer.setReceiveTimeOut(200);
        messageDealer.connect("tcp://" + host + ":" + ZMQServer.PORT_MESSAGE);
    }

    private Metrics run(int messages, int length) throws InterruptedException {
        if (length < MetricsClient.minLength(messages)) {
            throw new IllegalArgumentException("The minimum length for sending " + messages +
                    "metrics messages is " + MetricsClient.minLength(messages) + " chars due " +
                    "to formatting overheads.");
        }

        Log.i(getTag(), "Running metrics (" + messages + "messages, length " + length + "chars)");

        // Track the send and receive timestamps for each message (in nanoseconds).
        long[] timeSend = new long[messages];
        long[] timeRecv = new long[messages];

        // Start the message listener thread.
        listenerThread = new ListenerThread(timeRecv);
        listenerThread.start();

        // Wait for the server to accept the listener connection.
        Thread.sleep(100);

        // Send the metrics messages.
        sendMessages(messages, length, timeSend);

        // Wait for the last message to arrive.
        Thread.sleep(3000);

        // Close and join the listener thread.
        listenerThread.closed = true;
        listenerThread.join();

        // Process the timing data to produce a metrics object.
        return MetricsClient.process(timeSend, timeRecv, length);
    }

    private void sendMessages(int messages, int length, long[] timeSend) {
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
            messageDealer.send(msgBuilder.toString());
        }
    }

    private void disconnect() {
        // Close the message dealer and the context.
        messageDealer.close();
        context.term();

        // Drop references.
        listenerThread = null;
        messageDealer = null;
        context = null;
    }

    @Override
    public Metrics runMetrics(int messages, int length) throws IncompleteMetricsException {
        // Connect to the metrics server.
        connect();

        // Run the metrics, keeping the result.
        Metrics metrics;
        try {
            metrics = run(messages, length);
        } catch (InterruptedException e) {
            return null;
        }

        // Disconnect from the metrics server.
        disconnect();

        return metrics;
    }

    @Override
    public String getName() {
        return "zmq";
    }

    private static String getTag() {
        return "METRICS_CLIENT_ZMQ";
    }
}
