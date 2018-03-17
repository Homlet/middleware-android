package uk.ac.cam.seh208.middleware.core.network.impl;

import org.zeromq.ZMQ;

import java.net.UnknownHostException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import uk.ac.cam.seh208.middleware.core.network.Address;
import uk.ac.cam.seh208.middleware.core.network.RequestContext;
import uk.ac.cam.seh208.middleware.core.network.RequestStream;
import uk.ac.cam.seh208.middleware.core.network.Responder;

/**
 * Provides a context for exchanging requests (used to implement control messages
 * for the internal operation of the middleware). This uses ROUTER/DEALER sockets
 * with a request/response pattern due to the asymmetric nature of control messages.
 */
public class ZMQRequestContext implements RequestContext {

    /**
     * The default port on which the incoming control message socket resides.
     */
    private static final int PORT_DEFAULT = 4852;

    /**
     * The number of I/O threads used by ZMQ to handle middleware communication.
     */
    private static final int IO_THREADS = 2;


    /**
     * ZMQ context encapsulating all sockets.
     */
    private final ZMQ.Context context;

    /**
     * Listener thread for the request context.
     */
    private final Thread requestServer;

    /**
     * Responder used for handling requests in the middleware layer.
     */
    private Responder responder;

    /**
     * Address on which the ROUTER socket should be bound.
     */
    private ZMQAddress localAddress;

    /**
     * Track whether the context has been terminated.
     */
    private boolean terminated;

    /**
     * Lock for safe implementation of termination.
     */
    private ReentrantReadWriteLock termLock;


    /**
     * Instantiate a new context with the given port for the ROUTER socket.
     */
    public ZMQRequestContext(ZMQSchemeConfiguration configuration) {
        // Create a new ZMQ context.
        context = ZMQ.context(IO_THREADS);
        termLock = new ReentrantReadWriteLock(true);

        // Compute the local address.
        // TODO: determine all local interface addresses, and use a sub-location.
        ZMQAddress.Builder addressBuilder = new ZMQAddress.Builder();
//        try {
        addressBuilder.setHost("*");//ZMQAddress.getLocalHost());
//        } catch (UnknownHostException e) {
//            // Default to all interfaces.
//            addressBuilder.setHost("*");
//        }
        localAddress = addressBuilder.setPort(configuration.getPort()).build();

        // Set-up the request/response context.
        responder = new Responder();
        requestServer = ZMQRequestServer.makeThread(context, localAddress, responder);
        requestServer.start();
    }

    /**
     * Return a request stream with the RPC semantics described in
     * RequestContext#getRequestStream
     *
     * The underlying implementation of the stream will use ROUTER and DEALER
     * sockets, organised as in asynchronous server pattern described in
     * chapter 3 of the official ZeroMQ guide.
     *
     * @param address ZeroMQ address on which the remote middleware instance resides.
     *
     * @return a reference to a ZMQRequestStream for sending requests to the remote host.
     */
    @Override
    public RequestStream getRequestStream(Address address) {
        synchronized (termLock.readLock()) {
            if (terminated) {
                return null;
            }

            if (!(address instanceof ZMQAddress)) {
                return null;
            }
            ZMQAddress zmqAddress = (ZMQAddress) address;

            // Open a new request stream to the given remote host.
            return new ZMQRequestStream(context, localAddress, zmqAddress);
        }
    }

    /**
     * Return a responder for servicing incoming RPC requests.
     *
     * @return a reference to a Responder for servicing incoming requests
     *         over the ZMQ context, or null in the case of error.
     */
    @Override
    public Responder getResponder() {
        return responder;
    }

    /**
     * Terminate the context, closing all open streams and preventing new streams
     * from being opened in the future.
     */
    public void term() {
        synchronized (termLock.writeLock()) {
            if (terminated) {
                return;
            }

            context.term();
            requestServer.interrupt();

            terminated = true;
        }
    }
}
