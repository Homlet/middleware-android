package uk.ac.cam.seh208.middleware.core.comms.impl;

import android.util.Log;

import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java8.util.Sets;
import uk.ac.cam.seh208.middleware.common.JSONSerializable;
import uk.ac.cam.seh208.middleware.core.exception.NoValidAddressException;
import uk.ac.cam.seh208.middleware.core.comms.Address;
import uk.ac.cam.seh208.middleware.core.comms.Environment;
import uk.ac.cam.seh208.middleware.core.comms.Location;

import static uk.ac.cam.seh208.middleware.core.comms.Address.SCHEME_ZMQ;


/**
 * Implementation of the server component of the Harmony pattern.
 */
class ZMQMessageServer implements Runnable {

    /**
     * Instantiate a new ZMQMessageServer object with the given parameters, and return
     * a new Thread using its behaviour.
     *
     * @param environment Reference to the environment owning this server.
     * @param context Context in which to open the ROUTER socket.
     * @param state Store of associated state.
     * @param port Port number on which the ROUTER socket is bound.
     *
     * @return a newly instantiated Thread object.
     */
    static Thread makeThread(Environment environment, ZMQ.Context context,
                             ZMQMessageState state, int port) {
        return new Thread(new ZMQMessageServer(environment, context, state, port));
    }


    /**
     * Reference to the environment owning this server.
     */
    private Environment environment;

    /**
     * ZeroMQ context in which to open the ROUTER socket.
     */
    private ZMQ.Context context;

    /**
     * Store of state associated with the Harmony context.
     */
    private ZMQMessageState state;

    /**
     * Port number on which the ROUTER socket is bound.
     */
    private int port;


    /**
     * Store the passed parameters in preparation for operation.
     *
     * @param environment Reference to the environment owning this server.
     * @param context Context in which to open the ROUTER socket.
     * @param state Store of associated state.
     * @param port Port number on which the ROUTER socket is bound.
     */
    private ZMQMessageServer(Environment environment, ZMQ.Context context,
                             ZMQMessageState state, int port) {
        this.environment = environment;
        this.context = context;
        this.state = state;
        this.port = port;
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
            router.bind("tcp://*:" + port);

            // NOTE: This loop is a hot spot of the middleware, as all incoming
            //       messages (over TCP) pass through it on the same thread.
            while (!context.isClosed()) {
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
                    // If the peer identity is not tracked in the state, this
                    // must be an initial message.
                    try {
                        Log.v(getTag(), "INIT: \"" + data + "\"");
                        ZMQInitialMessage initialMessage =
                                JSONSerializable.fromJSON(data, ZMQInitialMessage.class);

                        stream = resolve(initialMessage);

                        if (stream == null) {
                            Log.w(getTag(), "Couldn't resolve stream for incoming message.");
                            continue;
                        }

                        Log.d(getTag(), "Resolved stream " + stream.getRemote());

                        // Now we have resolved the stream, associate it with the ROUTER identity.
                        state.insertStreamByIdentity(identity, stream);
                    } catch (IOException e) {
                        Log.e(getTag(), "Failed to parse initial message: \"" + data + "\"");
                    } catch (NoValidAddressException e) {
                        Log.e(getTag(), "Failed to create new stream: no valid return address.");
                    }
                } else {
                    // The stream has already been set up to receive from this peer.

                    // Check if we have a FIN message.
                    if (data.isEmpty()) {
                        // If so, remove the stream from the state entirely.
                        state.removeStreamByAddress(stream.getRemote());
                        state.removeStreamByIdentity(identity);

                        // Close the stream if not already closed, logging the FIN event.
                        if (stream.isClosed()) {
                            Log.v(stream.getTag(), "FIN (ACK)");
                        } else {
                            Log.v(stream.getTag(), "FIN");
                            stream.close();
                        }
                    } else {
                        // Otherwise, direct the message to the listeners of the stream.
                        stream.onMessage(data);
                    }
                }
            }
        } catch (ZMQException e) {
            if (e.getErrorCode() != ZMQ.Error.ETERM.getCode()) {
                // This was not thrown due to context termination.
                Log.e(getTag(), "Fatal error in message server", e);
            }
        }

        Log.i(getTag(), "Message server terminated.");
    }

    /**
     * Resolve the correct peer address for a new stream according to the contents of the
     * initial message. The initial message is sent from newly connected peers and contains
     * all interface addresses on which the peer is bound. If no local stream ends have been
     * opened for any of these, a new one is opened. If exactly one has been opened, it is
     * returned. If more than one contender is open, a one is selected and the other is
     * merged into it.
     *
     * @param initial The initial message.
     *
     * @return The resolved local stream end for the peer.
     *
     * @throws NoValidAddressException when no valid ZeroMQ addresses are included in
     *                                 the initial message.
     */
    private ZMQMessageStream resolve(ZMQInitialMessage initial) throws NoValidAddressException {

        Location location = initial.getLocation();

        // Attempt to find a local stream that has already been opened
        // for this location.
        List<ZMQMessageStream> contenders = new ArrayList<>();
        for (Address address : location) {
            if (!(address instanceof ZMQAddress)) {
                // Ignore addresses from a different scheme; dealing with
                // aliasing of these is solved by the environment.
                continue;
            }

            // Attempt to find an existing message stream to the remote host.
            ZMQAddress zmqAddress = (ZMQAddress) address;
            ZMQMessageStream contender = state.getStreamByAddress(zmqAddress);
            if (contender != null) {
                contenders.add(contender);
            }
        }

        if (contenders.size() == 0) {
            // If there are no contenders, this is the first time we've communicated
            // with the remote location. Create a new stream in the state for it,
            // registering it under its highest priority ZeroMQ address.

            ZMQAddress zmqAddress =
                    (ZMQAddress) location.priorityAddressForSchemes(Sets.of(SCHEME_ZMQ));
            ZMQMessageStream stream = new ZMQMessageStream(environment, context, zmqAddress);
            state.insertStreamByAddress(zmqAddress, stream);
            return stream;
        }

        if (contenders.size() == 1) {
            // If there is only one contender, we have already opened our side of
            // the stream to the remote location.

            return contenders.get(0);
        }

        // If there are multiple contenders, we have already opened our side of
        // the stream multiple times, under different aliased addresses.

        // TODO: figure out the best course of action here, if any.
        return null;
    }

    private String getTag() {
        return "MSG_SERVER[" + port + "]";
    }
}
