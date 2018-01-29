package uk.ac.cam.seh208.middleware.core.network.impl;

import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.util.ArrayList;
import java.util.Random;


/**
 * Implementation of the server component of the Harmony pattern.
 */
public class ZMQRequestServer implements Runnable {

    /**
     * The size of the thread pool created for responding to incoming requests.
     */
    public static final int RESPONDER_THREADS = 8;


    /**
     * Instantiate a new ZMQRequestServer object with the given parameters, and return
     * a new Thread using its behaviour.
     *
     * @param context Context in which to open the ROUTER socket.
     * @param state Store of associated state.
     *
     * @return a newly instantiated Thread object.
     */
    public static Thread makeThread(ZMQ.Context context, ZMQRequestState state) {
        return new Thread(new ZMQRequestServer(context, state));
    }


    /**
     * ZeroMQ context in which to open the ROUTER socket.
     */
    private ZMQ.Context context;

    /**
     * Store of state associated with the request context.
     */
    private ZMQRequestState state;


    /**
     * Store the passed parameters in preparation for operation.
     *
     * @param context Context in which to open the ROUTER socket.
     * @param state Store of associated state.
     */
    protected ZMQRequestServer(ZMQ.Context context, ZMQRequestState state) {
        this.context = context;
        this.state = state;
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
        ZMQResponder responder = state.getResponder();
        ArrayList<Thread> workers = new ArrayList<>();

        // Open the two ZeroMQ sockets.
        try (ZMQ.Socket router = context.socket(ZMQ.ROUTER);
             ZMQ.Socket dealer = context.socket(ZMQ.DEALER)) {
            // Set up the outward facing ROUTER socket.
            router.setRouterMandatory(true);
            router.bind("tcp://" + state.getLocalAddress());

            // Set up the inward facing DEALER socket.
            Random random = new Random(System.nanoTime());
            int internalId = random.nextInt();
            dealer.bind("inproc://req_" + internalId);

            // Spin up a number of worker threads to respond to dealt requests.
            // TODO: make these reliable in face of application code (request handler) failure.
            for (int i = 0; i < RESPONDER_THREADS; i++) {
                Thread worker = new Thread(() -> {
                    // Open a new reply socket for receiving requests from the dealer.
                    try (ZMQ.Socket socket = context.socket(ZMQ.REP)) {
                        socket.connect("inproc://req_" + internalId);

                        // Loop, handling requests.
                        while (!Thread.currentThread().isInterrupted()) {
                            socket.send(responder.respond(socket.recvStr()));
                        }
                    } catch (ZMQException ignore) {
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
            e.printStackTrace();  // TODO: logging without Android dependencies.
        } finally {
            try {
                // Interrupt and join all worker threads.
                for (Thread worker : workers) {
                    if (worker != null) {
                        worker.interrupt();
                        worker.join();
                    }
                }
            } catch (InterruptedException ignored) {
                // Give up on joining threads.
            }
        }
    }
}
