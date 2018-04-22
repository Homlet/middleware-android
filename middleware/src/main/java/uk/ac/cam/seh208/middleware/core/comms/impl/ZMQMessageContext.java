package uk.ac.cam.seh208.middleware.core.comms.impl;

import org.zeromq.ZMQ;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import uk.ac.cam.seh208.middleware.core.exception.MalformedAddressException;
import uk.ac.cam.seh208.middleware.core.comms.Address;
import uk.ac.cam.seh208.middleware.core.comms.Environment;
import uk.ac.cam.seh208.middleware.core.comms.MessageContext;
import uk.ac.cam.seh208.middleware.core.comms.MessageStream;


/**
 * Implements the Harmony pattern for peer-to-peer communication supporting message
 * transfer. This pattern is encapsulated within the context and within the returned
 * MessageStream objects. A ROUTER socket is maintained and bound to a well known port for
 * incoming connections. All messages intended for this middleware instance pass through
 * this ROUTER socket. Messages emitted from endpoints on this middleware pass through
 * individual DEALER sockets on ephemeral ports, which are created either when a message
 * is sent, or when a handshake message is received via the ROUTER. For more information
 * on the Harmony pattern, see the official ZeroMQ guide (chapter 8).
 */
public class ZMQMessageContext implements MessageContext {


    /**
     * The number of I/O threads used by ZMQ to handle middleware communication.
     */
    private static final int IO_THREADS = 2;


    /**
     * Reference to the environment owning this context.
     */
    private final Environment environment;

    /**
     * ZMQ context encapsulating all sockets.
     */
    private final ZMQ.Context context;

    /**
     * Port on which the Harmony server is bound.
     */
    private final int port;

    /**
     * Listener thread for the Harmony message context.
     */
    private final Thread harmonyServer;

    /**
     * Store of state associated with the Harmony context.
     */
    private final ZMQMessageState messageState;

    /**
     * Track whether the context has been terminated.
     */
    private boolean terminated;

    /**
     * Lock for safe implementation of termination.
     */
    private final ReentrantReadWriteLock termLock;


    /**
     * Instantiate a new context with the given port for the ROUTER socket.
     */
    public ZMQMessageContext(Environment environment, ZMQSchemeConfiguration configuration) {
        this.environment = environment;
        port = configuration.getPort();

        // Create a new ZMQ context.
        context = ZMQ.context(IO_THREADS);
        terminated = false;
        termLock = new ReentrantReadWriteLock(true);

        // Set-up the Harmony context.
        messageState = new ZMQMessageState();
        harmonyServer = ZMQMessageServer.makeThread(environment, context, messageState, port);
        harmonyServer.start();
    }

    /**
     * Return a list of addresses on which the Harmony server is bound.
     */
    @Override
    public List<Address> getInterfaceAddresses() {
        List<Address> output = new ArrayList<>();

        Enumeration<NetworkInterface> ifaces;
        try {
            ifaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            // If we could not retrieve the network interface list, we
            // probably can't bind to any interface addresses either.
            return Collections.emptyList();
        }

        for (NetworkInterface iface : Collections.list(ifaces)) {
            // List the addresses associated with each interface.
            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            for (InetAddress address : Collections.list(addresses)) {
                try {
                    // Make an address object from each interface address, and
                    // add it to the output list.
                    output.add(Address.make("zmq://" + address.getHostAddress() + ":" + port));
                } catch (MalformedAddressException ignored) {
                    // Should not be reachable.
                }
            }
        }

        return output;
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
     *
     * @param address ZeroMQ address on which the remote middleware instance is accessible.
     *
     * @return a reference to a ZMQMessageStream for sending messages to a
     *         remote instance of the middleware, or null in the case of error.
     */
    @Override
    public MessageStream getMessageStream(Address address) {
        synchronized (termLock.readLock()) {
            if (terminated) {
                return null;
            }

            if (!(address instanceof ZMQAddress)) {
                return null;
            }

            ZMQAddress zmqAddress = (ZMQAddress) address;

            // Retrieve the stream associated with this remote host from the state.
            // Use synchronization to prevent interleaving with the Harmony server code
            // that creates new streams when they do not already exist for incoming messages.
            synchronized (messageState) {
                ZMQMessageStream stream = messageState.getStreamByAddress(zmqAddress);

                // Without synchronization, the Harmony server could create a new stream in
                // the state here, which would then be overwritten by the stream in the
                // null case below.

                if (stream == null) {
                    // Instantiate a new stream in Harmony state.
                    stream = new ZMQMessageStream(environment, context, zmqAddress);
                    messageState.insertStreamByAddress(zmqAddress, stream);
                }

                return stream;
            }
        }
    }

    /**
     * Terminate the context, closing all open streams and preventing new streams
     * from being opened in the future.
     */
    @Override
    public void term() {
        synchronized (termLock.writeLock()) {
            if (terminated) {
                return;
            }

            messageState.closeAll();
            context.term();

            harmonyServer.interrupt();

            terminated = true;
        }
    }
}
