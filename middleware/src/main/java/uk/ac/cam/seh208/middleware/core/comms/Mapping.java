package uk.ac.cam.seh208.middleware.core.comms;

import java.util.List;

import uk.ac.cam.seh208.middleware.common.Persistence;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.core.CloseableSubject;


/**
 * Object storing the result of a mapping command called on an endpoint.
 *
 * Consists of a collections of channels from the local endpoint
 * to remote endpoints.
 *
 * The persistence level determines how the mapping handles failure; partial
 * failure is the closure of some channels within the mapping, while complete
 * failure is the closure of all channels, or the local middleware instance
 * being killed by the scheduler.
 */
public class Mapping extends CloseableSubject<Mapping> {

    /**
     * Back-reference to the owning local endpoint object.
     */
    private Endpoint local;

    /**
     * A copy of the query that was originally used to establish the mapping.
     * This will be used in some way to re-establish the mapping when it fails,
     * depending on the persistence level.
     */
    private Query query;

    /**
     * The level of persistence specified when establishing the mapping. This
     * determines what efforts the middleware should make to restore the mapping
     * should it fail at some point.
     */
    private Persistence persistence;

    /**
     * Integer tracking the count of currently open channels in the mapping. This is used
     * to determine how many new channels should be established to restore the mapping.
     */
    private int open;


    /**
     * Construct a new mapping object, subscribing to every channel from the given
     * list as an open channel.
     *
     * @param query Query object to be stored for restoration.
     * @param persistence Persistence level determining restoration strategy.
     * @param channels List of channels which should be included as part of the mapping.
     */
    public Mapping(Endpoint local, Query query, Persistence persistence, List<Channel> channels) {
        this.local = local;
        this.query = query;
        this.persistence = persistence;

        // Add each channel as an open constituent of the mapping.
        if (channels == null) {
            return;
        }
        for (Channel channel : channels) {
            addChannel(channel);
        }
    }

    /**
     * Add a channel to the mapping, subscribing to its closure for
     * restoration purposes.
     *
     * @param channel Reference to the channel to add.
     */
    public synchronized void addChannel(Channel channel) {
        if (channel == null) {
            // We cannot subscribe to a null object.
            return;
        }

        // Attempt to subscribe to the channel. If the channel was closed
        // prior to this point, continue with no action.
        if (channel.subscribeIfOpen(this::onChannelClose)) {
            // Subscription was successful; increase the open channel count.
            open++;
        }
    }

    @Override
    public void close() {
        // Drop the endpoint reference to speed up garbage collection.
        local = null;

        // TODO: close all owned channels.

        super.close();
    }

    /**
     * Called by observed channels on closure.
     *
     * Mapping restoration logic is rooted here, meaning on channel closure
     * persistent mappings are immediately restored by the middleware.
     *
     * @param channel Reference to the channel which closed.
     */
    private synchronized void onChannelClose(Channel channel) {
        open--;
        restore();
    }

    /**
     * Implementation of mapping restoration logic.
     */
    private void restore() {
        // Switch restoration strategy depending on persistence level.
        switch (persistence) {
            case NONE:
                return;

            case RESEND_QUERY:
                if (open == 0) {
                    // TODO: implement.
                }
                return;

            case RESEND_QUERY_INDIVIDUAL:
                // TODO: implement.
                return;

            default:
                // TODO: support for EXACT persistence (stretch goal).
        }
    }
}
