package uk.ac.cam.seh208.middleware.core.network.impl;

import android.util.Log;

import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

import uk.ac.cam.seh208.middleware.core.exception.MalformedAddressException;


/**
 * Implementation of the server component of the Harmony pattern.
 */
class ZMQMessageServer implements Runnable {

    /**
     * Instantiate a new ZMQMessageServer object with the given parameters, and return
     * a new Thread using its behaviour.
     *
     * @param context Context in which to open the ROUTER socket.
     * @param state Store of associated state.
     *
     * @return a newly instantiated Thread object.
     */
    static Thread makeThread(ZMQ.Context context, ZMQMessageState state) {
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
    private ZMQMessageServer(ZMQ.Context context, ZMQMessageState state) {
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
                String identity = message.pop().toString();
                String data = message.pop().toString();
                message.destroy();

                // Retrieve the stream associated with this peer identity from the state.
                ZMQMessageStream stream = state.getStreamByIdentity(identity);

                if (stream == null) {
                    // TODO: use full location instead of just address.
                    // If the peer identity is not tracked in the state, this
                    // must be an initial message: extract the remote address
                    // from it.
                    ZMQAddress remoteAddress = new ZMQAddress.Builder()
                            .fromString(data)
                            .build();

                    // See if we've already created a stream for this address.
                    stream = state.getStreamByAddress(remoteAddress);
                    if (stream == null) {
                        // This is the first time we've communicated with this address.
                        // Create a new stream in the state for it.
                        stream = new ZMQMessageStream(
                                context, state.getLocalAddress(), remoteAddress);
                        state.insertStreamByAddress(remoteAddress, stream);
                    }

                    // Now we have resolved the stream, associate it with the ROUTER identity.
                    state.insertStreamByIdentity(identity, stream);
                } else {
                    // The stream has already been set up to receive from this peer.

                    // Check if we have a FIN message.
                    if (data.isEmpty()) {
                        // If so, remove the stream from the state entirely.
                        state.removeStreamByAddress(stream.getRemoteAddress());
                        state.removeStreamByIdentity(identity);

                        // Close the stream if not already closed, logging the FIN event.
                        if (stream.isClosed()) {
                            Log.d(stream.getTag(), "FIN (ACK)");
                        } else {
                            Log.d(stream.getTag(), "FIN");
                            stream.close();
                        }
                    } else {
                        // Otherwise, direct the message to the listeners of the stream.
                        stream.onMessage(data);
                    }
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
