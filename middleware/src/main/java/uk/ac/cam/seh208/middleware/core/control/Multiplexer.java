package uk.ac.cam.seh208.middleware.core.control;

import android.util.Log;
import android.util.LongSparseArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.Polarity;
import uk.ac.cam.seh208.middleware.common.exception.BadHostException;
import uk.ac.cam.seh208.middleware.core.BuildConfig;
import uk.ac.cam.seh208.middleware.core.CloseableSubject;
import uk.ac.cam.seh208.middleware.core.MiddlewareService;
import uk.ac.cam.seh208.middleware.core.exception.ConnectionFailedException;
import uk.ac.cam.seh208.middleware.core.comms.MessageListener;
import uk.ac.cam.seh208.middleware.core.comms.MessageStream;


/**
 * Responsible for multiplexing and de-multiplexing messages over a message stream.
 * This provides scope to change the underlying medium via MessageStream implementors,
 * which allow for nice communication semantics such as attempted re-establishing of
 * the underlying connection before propagating failure to channels.
 *
 * The multiplexer also provides deduplication between a number of channels which share a
 * local endpoint and destination host, but different destination endpoints.
 */
public class Multiplexer extends CloseableSubject<Multiplexer> {

    /**
     * MessageStream implementor providing the underlying communications to the remote middleware.
     */
    private MessageStream messageStream;


    /**
     * Back-reference to the owning service.
     */
    private MiddlewareService service;

    /**
     * Indicator of whether this is a local loopback multiplexer.
     */
    private boolean loopback;

    /**
     * Location and identifier of the remote instance of the middleware.
     */
    private Middleware remote;

    /**
     * Map of currently open channels carried by the multiplexer, indexed by their identifier.
     * This is used to determine whether a timeout closure of the multiplexer should be
     * scheduled on channel closure, and to de-multiplex incoming messages between endpoints.
     */
    private LongSparseArray<Channel> channels;

    /**
     * Map of currently open channels carried by the multiplexer, grouped by the identifier
     * of their local endpoint. This is used to determine which channel identifiers should be
     * prepended to each message for multiplexing purposes.
     */
    private LongSparseArray<List<Channel>> channelsByLocalEndpoint;

    /**
     * Lambda reference to the onMessage method for registering with the message stream.
     */
    private MessageListener listener;


    Multiplexer(MiddlewareService service, Middleware remote) throws BadHostException {
        this.service = service;
        this.messageStream = service.getMessageStream(remote.getMessageLocation());
        this.remote = remote;
        channels = new LongSparseArray<>();
        channelsByLocalEndpoint = new LongSparseArray<>();

        // If the local and remote middlewares are equal, this is a loopback multiplexer.
        loopback = service.getMiddleware().equals(remote);

        // Register the onMessage method as a message listener for the message stream.
        listener = this::onMessage;
        messageStream.registerListener(listener);

        // Attempt to subscribe to message stream closure, closing the
        // multiplexer when this occurs.
        if (!messageStream.subscribeIfOpen(s -> close())) {
            // If the message stream is already closed, close the multiplexer.
            close();
        }
    }

    /**
     * TODO: use read-write lock to allow concurrent sending and receiving.
     *
     * Carry a channel on this multiplexer. This requires that the channel
     * remote endpoint resides on the instance of the middleware this channel
     * communicates with.
     *
     * @return whether the channel was successfully carried.
     */
    synchronized boolean carryChannel(Channel channel) {
        if (isClosed()) {
            return false;
        }

        if (!Objects.equals(channel.getRemote().getMiddleware(), remote)) {
            Log.e(getTag(), "Attempted to carry an invalid channel.");
            return false;
        }

        channels.put(channel.getChannelId(), channel);

        // Find the channel list associated with the channel's local endpoint,
        // if one such list already exists, in order to add the channel to it.
        long localId = channel.getLocal().getEndpointId();
        if (channelsByLocalEndpoint.indexOfKey(localId) < 0) {
            // The list does not exist; create a new list and append the channel.
            List<Channel> channelsList = new ArrayList<>();
            channelsList.add(channel);
            channelsByLocalEndpoint.put(localId, channelsList);
        } else {
            // The list exists; add the channel to it.
            channelsByLocalEndpoint.get(localId).add(channel);
        }

        // Attempt to subscribe to channel closure, dropping the channel
        // when this occurs.
        if (!channel.subscribeIfOpen(this::dropChannel)) {
            // If the channel is already closed, drop it immediately.
            dropChannel(channel);
            return false;
        }

        return true;
    }

    /**
     * Stop carrying a previously carried channel. If this was the last channel
     * carried by this multiplexer, set a timeout for multiplexer closure.
     *
     * @return whether the channel was successfully dropped.
     */
    private synchronized boolean dropChannel(Channel channel) {
        if (isClosed()) {
            return false;
        }

        if (channels.indexOfKey(channel.getChannelId()) < 0) {
            Log.e(getTag(), "Attempted to remove channel not carried.");
            return false;
        }

        // Remove the channel from the channels map.
        channels.remove(channel.getChannelId());

        // Remove the channel from the channelsByLocalEndpoint list associated with
        // its local endpoint identifier.
        removeChannelByEndpoint(channel, channel.getLocal().getDetails());

        if (loopback) {
            // In the special case that both endpoints reside on the same middleware,
            // we must also remove the channel from the channelsByLocalEndpoint list
            // associated with its remote endpoint identifier.
            removeChannelByEndpoint(channel, channel.getRemote());
        }

        if (channels.size() == 0) {
            if (BuildConfig.DEBUG && channelsByLocalEndpoint.size() > 0) {
                throw new AssertionError("Inconsistent channel state in multiplexer.");
            }

            // If all carried channels are closed, set a timeout for the closure
            // of the multiplexer.
            // TODO: timeoutClose();
        }

        return true;
    }

    /**
     * Convenience procedure for removing a channel from the channelsByLocalEndpoint collection.
     * The given channel will (obviously) only be removed from the list pertaining to the given
     * endpoint if the endpoint actually resides in that list.
     *
     * @param channel The channel to remove.
     * @param details Details of the endpoint the channel is associated with.
     */
    private synchronized void removeChannelByEndpoint(Channel channel, EndpointDetails details) {
        long endpointId = details.getEndpointId();
        List<Channel> channelList = channelsByLocalEndpoint.get(endpointId);
        channelList.remove(channel);
        if (channelList.isEmpty()) {
            // If the list is now empty, remove it from the map.
            channelsByLocalEndpoint.remove(endpointId);
        }
    }

    /**
     * Send the message along the associated message stream, prepending the ids of all
     * carried channels associated with the originator endpoint.
     */
    public synchronized void send(Endpoint local, String data) {
        if (isClosed()) {
            return;
        }

        if (channelsByLocalEndpoint.indexOfKey(local.getEndpointId()) < 0) {
            Log.e(getTag(), "Attempted to send a message from a local endpoint with no " +
                    "carried channels.");
            return;
        }

        try {
            // Build a message efficiently using a StringBuilder object.
            StringBuilder message = new StringBuilder();
            for (Channel channel : channelsByLocalEndpoint.get(local.getEndpointId())) {
                // For each channel sharing the given local endpoint, prepend the
                // channel identifier to the message.
                message.append(channel.getChannelId());
                message.append("|");
            }

            if (BuildConfig.DEBUG && message.toString().isEmpty()) {
                throw new AssertionError("Bad state in channelsByLocalEndpoint");
            }

            // Delimit the prefix from the message data using a second newline.
            message.append("|");
            message.append(data);

            // Send the newly built message over the associated message stream.
            messageStream.send(message.toString());
        } catch (ConnectionFailedException e) {
            close();
        }
    }

    @Override
    public synchronized void close() {
        if (isClosed()) {
            return;
        }

        // Close (and thus drop) all remaining channels.
        // Channels being migrated to another multiplexer should be
        // dropped manually before closing this multiplexer, otherwise
        // they will be closed automatically here.
        int size = channels.size();
        for (int i = 0; i < size; i++) {
            channels.valueAt(0).close();
        }

        // Remove the message listener from the message stream.
        messageStream.unregisterListener(listener);

        super.close();
    }

    public Middleware getRemote() {
        return remote;
    }

    /**
     * On received raw message, split into prefix and data, and dispatch the data to
     * all local endpoints referenced by the channel identifiers in the prefix.
     */
    private synchronized void onMessage(String message) {
        if (isClosed()) {
            return;
        }

        int divider = message.indexOf("||");
        String[] parts = message.substring(0, divider).split("\\|");
        String data = message.substring(divider + 2);

        // Dispatch the message type and separated channel identifier to the local
        // endpoint of each of the addressed channels.
        for (String part : parts) {
            long channelId = Long.valueOf(part);

            if (channels.indexOfKey(channelId) < 0) {
                // If the channels map does not contain the channel id, we
                // probably shouldn't have received it.
                // TODO: stop dropping messages here when the channel has just been set up/closed.
                // TODO: respond telling remote to close the erroneous channel.
                Log.w(getTag(), "Received message for unknown channel (" + channelId + ")");
                continue;
            }

            Endpoint sink = channels.get(channelId).getLocal();
            if (loopback) {
                // If this is a loopback multiplexer, choose the endpoint having the sink
                // polarity. This is a bit of a hack, and the state space of the multiplexer
                // would have to be changed in the future to support more exotic polarities.
                if (sink.getPolarity() != Polarity.SINK) {
                    EndpointDetails details = channels.get(channelId).getRemote();
                    sink = service.getEndpointSet().getEndpointByName(details.getName());
                }
            }

            // Delegate to the message handler of the channel's local endpoint.
            sink.onMessage(channelId, data);
        }
    }

    private String getTag() {
        return "MUX[" + remote.getUUID() + "]";
    }
}
