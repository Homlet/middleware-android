package uk.ac.cam.seh208.middleware.metrics;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class TCPServer implements MetricsServer {

    private class ListenerThread extends Thread {

        volatile boolean closed = false;


        @Override
        public void run() {
            ServerSocket server = null;
            Socket peer = null;
            try {
                // Create and bind the message server.
                server = new ServerSocket(PORT);

                Log.d(getTag(), "Listener thread running.");

                while (!closed) {
                    // Get connections from peers sequentially.
                    peer = server.accept();
                    pipe(peer.getInputStream(), peer.getOutputStream());
                    peer.close();
                }
            } catch (InterruptedIOException ignored) {
                // Pass through to the finally block.
            } catch (IOException e) {
                Log.e(getTag(), "Error in listener thread", e);
            } finally {
                Log.d(getTag(), "Listener thread stopped.");

                // Close the server socket, and the current peer if open.
                if (server != null) {
                    try {
                        server.close();
                    } catch (IOException ignored) {
                        // Do nothing.
                    }
                }

                if (peer != null) {
                    try {
                        peer.close();
                    } catch (IOException ignored) {
                        // Do nothing.
                    }
                }
            }
        }

        private void pipe(InputStream is, OutputStream os) throws IOException {
            int n;
            byte[] buffer = new byte[4096];
            while (!closed && (n = is.read(buffer)) > -1) {
                os.write(buffer, 0, n);
            }
        }
    }


    static final int PORT = 5205;


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
        listenerThread.interrupt();
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
        return "METRICS_SERVER_TCP";
    }
}
