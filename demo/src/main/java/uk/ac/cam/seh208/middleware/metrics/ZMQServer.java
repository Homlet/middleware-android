package uk.ac.cam.seh208.middleware.metrics;

import android.util.Log;

import org.zeromq.ZMQ;

import static org.zeromq.ZMQ.DEALER;


public class ZMQServer implements MetricsServer {

    private class ListenerThread extends Thread {

        private static final int IO_THREADS = 2;


        volatile boolean closed = false;


        @Override
        public void run() {
            ZMQ.Context context = ZMQ.context(IO_THREADS);

            // Create and bind the message dealer.
            ZMQ.Socket messageDealer = context.socket(DEALER);
            messageDealer.setReceiveTimeOut(200);
            messageDealer.bind("tcp://*:" + PORT_MESSAGE);

            // Create and bind the receipt dealer.
            ZMQ.Socket receiptDealer = context.socket(DEALER);
            receiptDealer.setSendTimeOut(200);
            receiptDealer.bind("tcp://*:" + PORT_RECEIPT);

            Log.d(getTag(), "Listener thread running.");

            // Send receipts back to the client.
            while (!closed) {
                byte[] msg = messageDealer.recv();
                if (msg == null) {
                    // The recv call timed out. Check the loop condition and continue.
                    continue;
                }
                receiptDealer.send(msg);
            }

            // Close the sockets and context.
            messageDealer.close();
            receiptDealer.close();
            context.term();

            Log.d(getTag(), "Listener thread stopped.");
        }
    }


    static final int PORT_MESSAGE = 5203;

    static final int PORT_RECEIPT = 5204;


    private ListenerThread listenerThread;

    private volatile boolean started = false;


    public synchronized void start() {
        if (started) {
            return;
        }

        listenerThread = new ListenerThread();
        listenerThread.start();

        Log.i(getTag(), "Started metrics server.");
        started = true;
    }

    public synchronized void stop() {
        if (!started) {
            return;
        }

        listenerThread.closed = true;
        try {
            listenerThread.join();
        } catch (InterruptedException ignored) {
            // Do nothing.
        }

        Log.i(getTag(), "Stopped metrics server.");
        started = false;
    }

    public boolean isStarted() {
        return started;
    }

    private static String getTag() {
        return "METRICS_SERVER_ZMQ";
    }
}
