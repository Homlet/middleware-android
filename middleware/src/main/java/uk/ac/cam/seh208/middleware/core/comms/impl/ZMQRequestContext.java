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
import uk.ac.cam.seh208.middleware.core.comms.RequestContext;
import uk.ac.cam.seh208.middleware.core.comms.RequestStream;
import uk.ac.cam.seh208.middleware.core.comms.Responder;

/**
 * Provides a context for exchanging requests (used to implement control messages
 * for the internal operation of the middleware). This uses ROUTER/DEALER sockets
 * with a request/response pattern due to the asymmetric nature of control messages.
 */
public class ZMQRequestContext implements RequestContext {

    /**
     * The number of I/O threads used by ZMQ to handle middleware communication.
     */
    private static final int IO_THREADS = 2;


    /**
     * ZMQ context encapsulating all sockets.
     */
    private final ZMQ.Context context;

    /**
     * Port on which the request server is bound.
     */
    private final int port;

    /**
     * Listener thread for the request context.
     */
    private final Thread requestServer;

    /**
     * Responder used for handling requests in the middleware layer.
     */
    private final Responder responder;

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
    public ZMQRequestContext(ZMQSchemeConfiguration configuration) {
        port = configuration.getPort();

        // Create a new ZMQ context.
        context = ZMQ.context(IO_THREADS);
        termLock = new ReentrantReadWriteLock(true);

        // Compute the local address.

        // Set-up the request/response context.
        responder = new Responder();
        requestServer = ZMQRequestServer.makeThread(context, responder, port);
        requestServer.start();
    }

    /**
     * Return a list of addresses on which the request server is bound.
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
                    output.add(Address.make("zmq://" + address + ":" + port));
                } catch (MalformedAddressException ignored) {
                    // Should not be reachable.
                }
            }
        }

        return output;
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
            return new ZMQRequestStream(context, zmqAddress);
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
