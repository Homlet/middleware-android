package uk.ac.cam.seh208.middleware.core.network.impl;

import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import uk.ac.cam.seh208.middleware.core.network.Address;
import uk.ac.cam.seh208.middleware.core.network.RequestStream;


/**
 * ZeroMQ implementor of the request stream interface.
 */
public class ZMQRequestStream extends RequestStream {

    /**
     * REQ socket over which to send new requests.
     */
    private ZMQ.Socket socket;

    /**
     * ZeroMQ context in which to open the REQ socket.
     */
    private ZMQ.Context context;

    // TODO: store a full location.
    /**
     * The ZeroMQ address on which this middleware instance resides.
     */
    private ZMQAddress localAddress;

    /**
     * The ZeroMQ address with which this stream communicates.
     */
    private ZMQAddress remoteAddress;


    public ZMQRequestStream(ZMQ.Context context, ZMQAddress localAddress,
                            ZMQAddress remoteAddress) {
        this.context = context;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public synchronized void close() {
        // We are already closed.
        if (isClosed()) {
            return;
        }

        // Disconnect from the peer (if connected).
        disconnect();

        super.close();
    }

    /**
     * Send a request message to the remote host over the request socket, opening
     * the socket if it does not yet exist. Should the stream be remotely closed,
     * or the remote host respond with an error, null is returned immediately.
     *
     * @param request String request to send to the remote middleware instance.
     *
     * @return the response string, or null in the case of an error.
     */
    @Override
    public synchronized String request(String request) {
        // We cannot send from a closed stream.
        if (isClosed()) {
            return null;
        }

        // If the REQ socket has not been opened, do so now.
        if (socket == null) {
            if (!connect()) {
                return null;
            }

            // The new socket has been created and connected successfully.
        }

        // Send the string request over the socket.
        socket.send(request);

        try {
            // Wait for a response from the peer.
            return socket.recvStr();
        } catch (ZMQException e) {
            // Indicate failure by returning null.
            return null;
        }
    }

    public ZMQAddress getLocalAddress() {
        return localAddress;
    }

    public ZMQAddress getRemoteAddress() {
        return remoteAddress;
    }


    /**
     * Open the REQ socket to the peer if this has not already been done. After
     * opening, an initialisation message is sent containing the local host.
     *
     * @return whether the DEALER socket exists and is connected to the peer. If not,
     *         the dealer field will remain null.
     */
    private synchronized boolean connect() {
        // We cannot re-establish a socket after the stream has been closed.
        if (isClosed()) {
            return false;
        }

        // If the socket has already been opened, do not open another.
        if (socket != null) {
            return true;
        }

        try {
            // Open a new REQ socket.
            socket = context.socket(ZMQ.REQ);

            // Attempt to connect the socket to the peer.
            socket.connect("tcp://" + remoteAddress.toCanonicalString());
        } catch (ZMQException e) {
            // Close and release the new socket.
            if (socket != null) {
                socket.close();
                socket = null;
            }

            return false;
        }

        return true;
    }

    /**
     * Close the DEALER socket to the peer if it is currently open, breaking
     * the connection to the peer.
     */
    private synchronized void disconnect() {
        // If the DEALER does not exist, there's nothing to close.
        if (socket == null) {
            return;
        }

        // Close and release the socket.
        socket.close();
        socket = null;
    }
}
