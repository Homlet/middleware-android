package uk.ac.cam.seh208.middleware.core.network;

import org.zeromq.ZMQ;

import java.net.InetAddress;
import java.net.UnknownHostException;


// TODO: split this in two; why on earth did I make this one big class to do two things!?
/**
 * Stores an overarching ZMQ connection context.
 *
 * Implements the Harmony pattern for peer-to-peer communication supporting message
 * transfer. This pattern is encapsulated within the context and within the returned
 * MessageStream objects. A ROUTER socket is maintained and bound to a well known port for
 * incoming connections. All messages intended for this middleware instance pass through
 * this ROUTER socket. Messages emitted from endpoints on this middleware pass through
 * individual DEALER sockets on ephemeral ports, which are created either when a message
 * is sent, or when a handshake message is received via the ROUTER. For more information
 * on the Harmony pattern, see the official ZeroMQ guide (chapter 8).
 *
 * Provides an additional context for exchanging requests (used to implement control messages
 * for the internal operation of the middleware). This uses ROUTER/DEALER sockets with a
 * request/response pattern due to the asymmetric nature of control messages.
 */
public class ZMQContext implements MessageContext, RequestContext {

    /**
     * Return the public IP of the local host.
     *
     * @return a String formatted IP address.
     *
     * @throws UnknownHostException if the host has no bound IP address.
     */
    public static String getLocalHost() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress();
    }

    /**
     * The port on which the incoming message socket resides.
     */
    public static final int PORT_MESSAGE = 4800;

    /**
     * The port on which the incoming control message socket resides.
     */
    public static final int PORT_REQUEST = 4801;

    /**
     * The number of I/O threads used by ZMQ to handle middleware communication.
     */
    private static final int IO_THREADS = 2;


    /**
     * ZMQ context encapsulating all sockets.
     */
    private ZMQ.Context context;

    /**
     * Store of Harmony state.
     */
    private HarmonyState harmonyState;

    /**
     * Listener thread for the Harmony message context.
     */
    private Thread harmonyServer;

    /**
     * Store of request/response state.
     */
    private ZMQRequestState requestState;

    /**
     * Listener thread for the request context.
     */
    private Thread requestServer;


    /**
     * Instantiate a new context with the given ports for the message and
     * request receiving server sockets.
     */
    public ZMQContext(int portMessage, int portRequest) {
        // Create a new ZMQ context.
        context = ZMQ.context(IO_THREADS);

        // Compute the local address.
        // TODO: determine all local interface addresses, and use a location.
        ZMQAddress.Builder addressBuilder = new ZMQAddress.Builder().setPort(portMessage);
        try {
            addressBuilder.setHost(getLocalHost());
        } catch (UnknownHostException e) {
            // Default to all interfaces.
            addressBuilder.setHost("*");
        }
        ZMQAddress localAddress = addressBuilder.build();

        // Set-up the Harmony context.
        harmonyState = new HarmonyState(localAddress);
        harmonyServer = HarmonyServer.makeThread(context, harmonyState);
        harmonyServer.start();

        // Set-up the request/response context.
        requestState = new ZMQRequestState(new ZMQResponder(), localAddress);
        requestServer = ZMQRequestServer.makeThread(context, requestState);
        requestServer.start();
    }

    /**
     * Convenience constructor for creating a new context with the default ports.
     */
    public ZMQContext() {
        this(PORT_MESSAGE, PORT_REQUEST);
    }

    /**
     * Return a message stream for communication with a remote instance of the
     * middleware, having the restrictions listed in MessageContext#getMessageStream
     *
     * The underlying implementation of the stream will follow the Harmony pattern
     * described in chapter 8 of the official ZeroMQ guide. Upon creation, the
     * initialisation message is sent, causing the remote context to instantiate
     * and cache a peer stream. Messages received on either before the middleware
     * has attached a listener are buffered in order.
     *
     * If a Harmony stream to the remote host already exists, a cached reference to
     * it will be returned instead of opening a new stream.
     * TODO: use a shim to cache streams across different contexts for the same remote.
     *
     * @param address ZeroMQ address on which the remote middleware instance is accessible.
     *
     * @return a reference to a ZMQMessageStream for sending messages to a
     *         remote instance of the middleware, or null in the case of error.
     */
    @Override
    public MessageStream getMessageStream(Address address) {
        if (!(address instanceof ZMQAddress)) {
            return null;
        }

        ZMQAddress zmqAddress = (ZMQAddress) address;

        // Retrieve the stream associated with this remote host from the state.
        HarmonyMessageStream stream = harmonyState.getStreamByAddress(zmqAddress);

        if (stream == null) {
            // Instantiate a new stream.
            // TODO: mark as unknown identity in the state, allowing the gap to be filled in later.
            stream = new HarmonyMessageStream(context, harmonyState.getLocalAddress(), zmqAddress);
        }

        return stream;
    }

    /**
     * Return a request stream with the RPC semantics described in
     * RequestContext#getRequestStream
     *
     * The underlying implementation of the stream will use ROUTER and DEALER
     * sockets, organised as in asynchronous server pattern described in
     * chapter 3 of the official ZeroMQ guide.
     *
     * If a request stream to the remote host already exists, a cached reference
     * to it will be returned instead of opening a new stream.
     * TODO: consider how to do asynchronous requests since the stream has blocking semantics.
     *
     * @param address ZeroMQ address on which the remote middleware instance resides.
     *
     * @return a reference to a ZMQRequestStream for sending requests to the
     *         remote host, or null in the case of error.
     */
    @Override
    public RequestStream getRequestStream(Address address) {
        if (!(address instanceof ZMQAddress)) {
            return null;
        }
        ZMQAddress zmqAddress = (ZMQAddress) address;

        // Open a new request stream to the given remote host.
        ZMQRequestStream stream = requestState.getStreamByAddress(zmqAddress);

        if (stream == null) {
            // Instantiate a new stream in the state.
            stream = new ZMQRequestStream(context, requestState.getLocalAddress(), zmqAddress);
            requestState.insertStream(zmqAddress, stream);
        }

        return stream;
    }

    /**
     * Return a responder for servicing incoming RPC requests.
     *
     * The underlying implementation of the stream will use ROUTER and DEALER
     * sockets, organised as in asynchronous server pattern described in
     * chapter 3 of the official ZeroMQ guide.
     *
     * @return a reference to a ZMQResponder for servicing incoming requests
     *         over the ZMQ context, or null in the case of error.
     */
    @Override
    public Responder getResponder() {
        return requestState.getResponder();
    }
}
