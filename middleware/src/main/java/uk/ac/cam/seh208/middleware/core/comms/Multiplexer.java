package uk.ac.cam.seh208.middleware.core.comms;

import android.util.Log;
import android.util.LongSparseArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import uk.ac.cam.seh208.middleware.BuildConfig;
import uk.ac.cam.seh208.middleware.common.exception.BadHostException;
import uk.ac.cam.seh208.middleware.core.CloseableSubject;
import uk.ac.cam.seh208.middleware.core.MiddlewareService;
import uk.ac.cam.seh208.middleware.core.exception.ConnectionFailedException;
import uk.ac.cam.seh208.middleware.core.network.Location;
import uk.ac.cam.seh208.middleware.core.network.MessageListener;
import uk.ac.cam.seh208.middleware.core.network.MessageStream;


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
     * Location on which the remote instance of the middleware resides.
     */
    private Location remote;

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


    public Multiplexer(MiddlewareService service, Location remote) throws BadHostException {
        this.messageStream = service.getMessageStream(remote);
        this.remote = remote;
        channels = new LongSparseArray<>();
        channelsByLocalEndpoint = new LongSparseArray<>();

        // Register the onMessage method as a message listener for the message stream.
        listener = this::onMessage;
        messageStream.registerListener(listener);

        // Attempt to subscribe to message stream closure, closing the
        // multiplexer when this occurs.
        if (!messageStream.subscribeIfOpen(s -> this.close())) {
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
    public synchronized boolean carryChannel(Channel channel) {
        if (isClosed()) {
            return false;
        }

        if (!Objects.equals(channel.getRemote().getLocation(), remote)) {
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
    public synchronized boolean dropChannel(Channel channel) {
        if (isClosed()) {
            return false;
        }

        if (channels.indexOfKey(channel.getChannelId()) < 0) {
            Log.e(getTag(), "Attempted to remove channel not carried.");
            return false;
        }

        // Remove the channel from the channels map.
        channels.remove(channel.getChannelId());

        // Get the channel list associated with the channel's local endpoint,
        // and remove the channel from it.
        long localId = channel.getLocal().getEndpointId();
        List<Channel> channelList = channelsByLocalEndpoint.get(localId);
        channelList.remove(channel);
        if (channelList.isEmpty()) {
            // If the list is now empty, remove it from the map.
            channelsByLocalEndpoint.remove(localId);
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
                message.append("\n");
            }

            if (BuildConfig.DEBUG && message.toString().isEmpty()) {
                throw new AssertionError("Bad state in channelsByLocalEndpoint");
            }

            // Delimit the prefix from the message data using a second newline.
            message.append("\n");
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

    public Location getRemote() {
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

        int divider = message.indexOf("\n\n");
        String[] parts = message.substring(0, divider).split("\n");
        String data = message.substring(divider + 2);

        // Dispatch the message type and separated channel identifier to the local
        // endpoint of each of the addressed channels.
        for (String part : parts) {
            long channelId = Long.valueOf(part);

            if (channels.indexOfKey(channelId) < 0) {
                // If the channels map does not contain the channel id, we
                // probably shouldn't have received it.
                // TODO: stop dropping messages here when the channel has just been set up.
                // TODO: respond telling remote to close the erroneous channel.
                Log.w(getTag(), "Received message for unknown channel (" + channelId + ")");
                continue;
            }

            // Delegate to the message handler of the channel's local endpoint.
            channels.get(channelId).getLocal().onMessage(channelId, data);
        }
    }

    private String getTag() {
        return "MUX[" + remote.getUUID() + "]";
    }
}
