package uk.ac.cam.seh208.middleware.core.network.impl;

import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

import uk.ac.cam.seh208.middleware.core.exception.MalformedAddressException;


/**
 * Implementation of the server component of the Harmony pattern.
 */
public class ZMQMessageServer implements Runnable {

    /**
     * Instantiate a new ZMQMessageServer object with the given parameters, and return
     * a new Thread using its behaviour.
     *
     * @param context Context in which to open the ROUTER socket.
     * @param state Store of associated state.
     *
     * @return a newly instantiated Thread object.
     */
    public static Thread makeThread(ZMQ.Context context, ZMQMessageState state) {
        return new Thread(new ZMQMessageServer(context, state));
    }


    /**
     * ZeroMQ context in which to open the ROUTER socket.
     */
    private ZMQ.Context context;

    /**
     * Store of state associated with the Harmony context.
     */
    private ZMQMessageState state;


    /**
     * Store the passed parameters in preparation for operation.
     *
     * @param context Context in which to open the ROUTER socket.
     * @param state Store of associated state.
     */
    protected ZMQMessageServer(ZMQ.Context context, ZMQMessageState state) {
        this.context = context;
        this.state = state;
    }

    /**
     * Implementation of the Harmony server.
     */
    @Override
    public void run() {
        // Instantiate the ROUTER socket within the stored context,
        // and bind it on the stored port.
        try (ZMQ.Socket router = context.socket(ZMQ.ROUTER)) {
            router.setRouterMandatory(true);
            router.bind("tcp://" + state.getLocalAddress());

            // NOTE: This loop is a hot spot of the middleware, as all incoming
            //       messages (over TCP) pass through it on the same thread.
            while (!Thread.currentThread().isInterrupted()) {
                // Block to receive a message on the ROUTER socket.
                ZMsg message = ZMsg.recvMsg(router);
                if (message == null) {
                    // recvMsg returns null when interrupted.
                    break;
                }

                // Extract the identity of the peer, and the message
                // data from the peer.
                ZFrame peer = message.pop();
                ZFrame data = message.pop();
                message.destroy();

                // Retrieve the stream associated with this peer identity from the state.
                ZMQMessageStream stream = state.getStreamByIdentity(peer.toString());

                if (stream == null) {
                    // TODO: use full location instead of just address.
                    // If the peer identity is not tracked in the state, this
                    // must be an initial message: extract the remote address
                    // from it.
                    ZMQAddress remoteAddress = new ZMQAddress.Builder()
                            .fromString(data.toString())
                            .build();
                    // See if we've already created a stream for this address.
                    stream = state.getStreamByAddress(remoteAddress);
                    if (stream == null) {
                        // This is the first time we've communicated with this address.
                        // Create a new stream in the state and immediately associate
                        // the identity with it.
                        stream = new ZMQMessageStream(
                                context, state.getLocalAddress(), remoteAddress);
                        state.insertStream(remoteAddress, peer.toString(), stream);
                    } else {
                        // We've already created a stream to this address, but we didn't
                        // previously have a ROUTER identity for it. Track this in the
                        // state.
                        state.trackIdentity(remoteAddress, peer.toString());
                    }
                } else {
                    // Otherwise, direct the message to the listeners of the relevant
                    // message stream, according to the identity of the peer.
                    stream.onMessage(data.toString());
                }
            }
        } catch (ZMQException e) {
            // This is some kind of network error.
            e.printStackTrace();  // TODO: logging without Android dependencies.
        } catch (MalformedAddressException e) {
            // The address received in the initial message was malformed.
            e.printStackTrace();
        }
    }
}
