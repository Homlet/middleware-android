package uk.ac.cam.seh208.middleware.core.network;

import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;


/**
 * Implementation of the server component of the Harmony pattern.
 */
public class HarmonyServer implements Runnable {

    /**
     * Instantiate a new HarmonyServer object with the given parameters, and return
     * a new Thread using its behaviour.
     *
     * @param context Context in which to open the ROUTER socket.
     * @param state Store of associated state.
     *
     * @return a newly instantiated Thread object.
     */
    public static Thread makeThread(ZMQ.Context context, HarmonyState state) {
        return new Thread(new HarmonyServer(context, state));
    }


    /**
     * ZeroMQ context in which to open the ROUTER socket.
     */
    private ZMQ.Context context;

    /**
     * Store of state associated with the Harmony context.
     */
    private HarmonyState state;


    /**
     * Store the passed parameters in preparation for operation.
     *
     * @param context Context in which to open the ROUTER socket.
     * @param state Store of associated state.
     */
    protected HarmonyServer(ZMQ.Context context, HarmonyState state) {
        this.context = context;
        this.state = state;
    }

    /**
     * Implementation of the Harmony server.
     */
    @Override
    public void run() {
        // Instantiate the router socket within the stored context,
        // and bind it on the stored port.
        try (ZMQ.Socket router = context.socket(ZMQ.ROUTER)) {
            router.setRouterMandatory(true);
            router.bind("tcp://" + state.getLocalAddress());

            // NOTE: This loop is a hot spot of the middleware, as all incoming
            //       messages (over TCP) pass through it on the same thread.
            while (Thread.currentThread().isInterrupted()) {
                // Block to receive a message on the router socket.
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

                // Retrieve the stream associated with this peer from the state.
                HarmonyMessageStream stream = state.getStreamByIdentity(peer.toString());

                if (stream == null) {
                    // TODO: use full location instead of just address.
                    // If the peer is not tracked in the peer table, this
                    // must be an initial message: create a new MessageStream
                    // in the peer table for it.
                    ZMQAddress remoteAddress = new ZMQAddress.Builder()
                            .fromString(data.toString())
                            .build();
                    stream = new HarmonyMessageStream(
                            context, state.getLocalAddress(), remoteAddress);
                    state.insertStream(peer.toString(), remoteAddress, stream);
                } else {
                    // Otherwise, direct the message to the listeners of the relevant
                    // message stream, according to the identity of the peer.
                    // TODO: implement.
                }
            }
        } catch (BadHarmonyStateException e) {
            // This is likely a bug in the implementation.
            e.printStackTrace();  // TODO: logging without Android dependencies.
        } catch (ZMQException e) {
            // This is some kind of network error.
            e.printStackTrace();
        } catch (MalformedAddressException e) {
            // The address received in the initial message was malformed.
            e.printStackTrace();
        }
    }
}
