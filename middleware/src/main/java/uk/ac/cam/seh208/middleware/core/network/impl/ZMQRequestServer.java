package uk.ac.cam.seh208.middleware.core.network.impl;

import android.util.Log;

import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.Random;

import uk.ac.cam.seh208.middleware.core.network.Responder;


/**
 * Implementation of the server component of the Harmony pattern.
 */
public class ZMQRequestServer implements Runnable {

    /**
     * The size of the thread pool created for responding to incoming requests.
     */
    private static final int RESPONDER_THREADS = 8;


    /**
     * Instantiate a new ZMQRequestServer object with the given parameters, and return
     * a new Thread using its behaviour.
     *
     * @param context Context in which to open the ROUTER socket.
     * @param localAddress Address on which the ROUTER socket should bind.
     * @param responder Responder used to handle incoming requests.
     *
     * @return a newly instantiated Thread object.
     */
    static Thread makeThread(ZMQ.Context context, ZMQAddress localAddress, Responder responder) {
        return new Thread(new ZMQRequestServer(context, localAddress, responder));
    }


    /**
     * ZeroMQ context in which to open the ROUTER socket.
     */
    private ZMQ.Context context;

    /**
     * Responder used for handling requests in the middleware layer.
     */
    private Responder responder;

    /**
     * Interface address on which the ROUTER should be bound.
     */
    private ZMQAddress localAddress;


    /**
     * Store the passed parameters in preparation for operation.
     *
     * @param context Context in which to open the ROUTER socket.
     */
    private ZMQRequestServer(ZMQ.Context context, ZMQAddress localAddress, Responder responder) {
        this.context = context;
        this.responder = responder;
        this.localAddress = localAddress;
    }

    /**
     * Implementation of the request server.
     *
     * A ROUTER socket is created for receiving requests from remote hosts,
     * and a DEALER is connected to it via a ZeroMQ proxy in order to
     * deal incoming requests between a number of worker threads.
     */
    @Override
    public void run() {
        ArrayList<Thread> workers = new ArrayList<>();

        // Open the two ZeroMQ sockets.
        try (ZMQ.Socket router = context.socket(ZMQ.ROUTER);
             ZMQ.Socket dealer = context.socket(ZMQ.DEALER)) {
            // Set up the outward facing ROUTER socket.
            router.setRouterMandatory(true);
            router.bind("tcp://" + localAddress);

            // Set up the inward facing DEALER socket.
            Random random = new Random(System.nanoTime());
            int internalId = random.nextInt();
            dealer.bind("inproc://req_" + internalId);

            // Spin up a number of worker threads to respond to dealt requests.
            for (int i = 0; i < RESPONDER_THREADS; i++) {
                Thread worker = new Thread(() -> {
                    // Open a new reply socket for receiving requests from the dealer.
                    try (ZMQ.Socket socket = context.socket(ZMQ.REP)) {
                        socket.connect("inproc://req_" + internalId);

                        // Loop, handling requests.
                        while (!Thread.currentThread().isInterrupted()) {
                            String request = socket.recvStr();
                            Log.d(getTag() + ".T" + Thread.currentThread().getId(),
                                    "REQ: \"" + request + "\"");
                            socket.send(responder.respond(request));
                        }
                    } catch (ZMQException ignored) {
                        // Either we were interrupted, or some network error occurred.
                    }
                });
                workers.add(worker);
                worker.start();
            }

            // Connect responder threads to the router via a queue.
            ZMQ.proxy(router, dealer, null);
        } catch (ZMQException e) {
            // This is some kind of network error.
            Log.e(getTag(), "Network error occurred.");
        } finally {
            Log.i(getTag(), "Terminating request server...");
            try {
                // Interrupt and join all worker threads.
                for (Thread worker : workers) {
                    if (worker != null) {
                        worker.join();
                    }
                }
            } catch (InterruptedException ignored) {
                // Give up on joining threads.
                Log.e(getTag(), "Could not join threads terminating request server.");
            }
        }
    }

    private String getTag() {
        return "REQ_SERVER[" + localAddress + "]";
    }
}
