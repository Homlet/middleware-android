package uk.ac.cam.seh208.middleware.core.control;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.core.CloseableSubject;


/**
 * An active channel for data to flow from one local endpoint to
 * another (remote), usually residing on a remote instance of the middleware.
 *
 * Channels will always exist in pairs, like socket endpoints.
 *
 * Channels do not actually implement the means for data transfer; this is
 * handled by the Multiplexer class. However, connection objects reference
 * channels in order to implement multiplexing.
 *
 * Once closed, a channel cannot be re-opened. Gracefully closing a channel sends
 * a tear-down control message to the remote middleware, so consensus is maintained
 * about the state of the channel. Channels are closed in the following situations:
 *     - (for channels maintained by a mapping) the channel's mapping is removed;
 *     - either (remote or local) endpoint is removed from its host middleware;
 *     - the local middleware is gracefully killed.
 *
 * Additionally, the following failure modes can cause a channel to be closed
 * is a non-graceful manner (i.e. without sending a tear-down message):
 *     - the underlying connection to the remote host fails while sending a message;
 *     - the local middleware is non-gracefully killed.
 *
 * In these situations, consensus about the state of the channel is not shared between
 * the local and remote middlewares. Therefore, these closures are tracked by the local
 * middleware and relayed to the remote middleware when possible, giving the final
 * condition for channel closure:
 *     - the remote middleware indicates that a channel was locally closed in the past,
 *       but it was not able to perform the graceful tear-down.
 */
public class Channel extends CloseableSubject<Channel> {

    /**
     * Generate a deterministic identifier based on the endpoints of the channel. This
     * identifier must be the same at both ends, so the details are sorted by identifier
     * before a hash is taken.
     */
    private static long generateId(EndpointDetails first, EndpointDetails second) {
        long multiplier = 769L;
        long constant = 12289L;
        if (first.getEndpointId() < second.getEndpointId()) {
            return multiplier * first.getEndpointId() + second.getEndpointId() + constant;
        } else {
            return multiplier * second.getEndpointId() + first.getEndpointId() + constant;
        }
    }


    /**
     * Universally unique identifier for the channel.
     */
    private long channelId;

    /**
     * Reference to the endpoint at the local end of the channel.
     */
    private Endpoint local;

    /**
     * Details of the endpoint at the remote end of the channel.
     */
    private RemoteEndpointDetails remote;


    /**
     * Create a new channel representing the flow of data between local and remote endpoints.
     *
     * @param local Reference to the endpoint object at the local end of the channel.
     * @param remote Details of the remote endpoint, including the host on which it resides.
     */
    Channel(Endpoint local, RemoteEndpointDetails remote) {
        channelId = generateId(local.getDetails(), remote);
        this.local = local;
        this.remote = remote;
    }

    long getChannelId() {
        return channelId;
    }

    public Endpoint getLocal() {
        return local;
    }

    public RemoteEndpointDetails getRemote() {
        return remote;
    }
}
