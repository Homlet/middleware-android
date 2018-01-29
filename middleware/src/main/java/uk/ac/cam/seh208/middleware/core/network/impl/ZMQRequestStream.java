package uk.ac.cam.seh208.middleware.core.network.impl;

import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.util.Objects;

import uk.ac.cam.seh208.middleware.core.CloseableSubject;
import uk.ac.cam.seh208.middleware.core.exception.MalformedAddressException;
import uk.ac.cam.seh208.middleware.core.network.Address;
import uk.ac.cam.seh208.middleware.core.network.RequestStream;


/**
 * ZeroMQ implementor of the request stream interface.
 */
public class ZMQRequestStream extends CloseableSubject<ZMQRequestStream> implements RequestStream {

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
    private Address localAddress;

    /**
     * The ZeroMQ address with which this stream communicates.
     */
    private Address remoteAddress;


    public ZMQRequestStream(ZMQ.Context context, ZMQAddress localAddress,
                            ZMQAddress remoteAddress) {
        this.context = context;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public synchronized void close() {
        // If the REQ socket was already created, close and release it.
        if (socket != null) {
            socket.close();
            socket = null;
        }

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

            // Attempt to send the initial request to the peer.
            // TODO: send complete location string rather than address.
            socket.send(localAddress.toCanonicalString());

            // Validate the response against the expected remote address.
            // TODO: validate uuid (not address), and update view on remote location.
            String response = socket.recvStr();
            ZMQAddress address = new ZMQAddress.Builder().fromString(response).build();
            if (Objects.equals(address, remoteAddress)) {
                // The address matches; our socket is ready for requests.
                return true;
            }
        } catch (MalformedAddressException | ZMQException ignored) {
            // The attempt failed. Continue for clean-up.
        }

        // Close and release the new socket.
        if (socket != null) {
            socket.close();
            socket = null;
        }

        return false;
    }
}
