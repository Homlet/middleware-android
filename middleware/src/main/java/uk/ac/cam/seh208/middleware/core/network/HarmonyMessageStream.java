package uk.ac.cam.seh208.middleware.core.network;

import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.net.UnknownHostException;

import uk.ac.cam.seh208.middleware.core.CloseableSubject;


/**
 * ZeroMQ Harmony-pattern implementor of the message stream interface.
 */
public class HarmonyMessageStream extends CloseableSubject<HarmonyMessageStream>
        implements MessageStream {

    /**
     * DEALER socket over which to send new messages.
     */
    private ZMQ.Socket dealer;

    /**
     * ZeroMQ context in which to open the DEALER socket.
     */
    private ZMQ.Context context;

    /**
     * The remote host with which this stream communicates.
     */
    private String host;


    public HarmonyMessageStream(ZMQ.Context context, String host) {
        this.context = context;
        this.host = host;
    }

    @Override
    public synchronized void close() {
        // If the DEALER socket was already created, close and release it.
        if (dealer != null) {
            dealer.close();
            dealer = null;
        }

        super.close();
    }

    @Override
    public synchronized void send(String message) {
        // We cannot send from a closed stream.
        if (isClosed()) {
            return;
        }

        // If the DEALER socket has not been opened, do so now.
        if (dealer == null) {
            if (!connect()) {
                return;
            }

            // The new socket has been created and connected successfully.
        }

        // Send the message over the DEALER socket.
        dealer.send(message);
    }

    @Override
    public synchronized void registerListener(MessageListener listener) {
        // TODO: implement.
    }

    @Override
    public synchronized void unregisterListener(MessageListener listener) {
        // TODO: implement.
    }

    @Override
    public synchronized void clearListeners() {
        // TODO: implement.
    }

    /**
     * Open the DEALER socket to the peer if this has not already been done. After
     * opening, an initialisation message is sent containing the local host.
     *
     * @return whether the DEALER socket exists and is connected to the peer. If not,
     *         the dealer field will remain null.
     */
    private synchronized boolean connect() {
        // We cannot re-establish a DEALER after the stream has been closed.
        if (isClosed()) {
            return false;
        }

        // If the DEALER socket has already been opened, do not open another.
        if (dealer != null) {
            return true;
        }

        try {
            // Open a new DEALER socket.
            dealer = context.socket(ZMQ.DEALER);

            // Attempt to connect the socket to the peer.
            dealer.connect("tcp://" + host);

            // Attempt to send the initial message to the peer.
            // TODO: proper initial message specification with multiple address spaces.
            dealer.send(ZMQContext.getLocalHost());
        } catch (ZMQException | UnknownHostException e) {
            // The attempt failed. Close and release the new socket.
            if (dealer != null) {
                dealer.close();
                dealer = null;
            }
            return false;
        }

        return true;
    }
}
