package uk.ac.cam.seh208.middleware.core.comms;

import uk.ac.cam.seh208.middleware.common.RemoteEndpointDetails;
import uk.ac.cam.seh208.middleware.core.CloseableSubject;


/**
 * An active channel for data to flow from one local endpoint (near) to
 * another (far), usually residing on a remote instance of the middleware.
 *
 * Channels will always exist in pairs, like socket endpoints.
 *
 * Channels do not actually implement the means for data transfer; this is
 * handled by the Connection class. However, connection objects reference
 * channels in order to implement multiplexing.
 *
 * Once closed, a channel cannot be re-opened. Gracefully closing a channel sends
 * a tear-down control message to the far middleware, so consensus is maintained
 * about the state of the channel. Channels are closed in the following situations:
 *     - (for channels maintained by a mapping) the channel's mapping is removed;
 *     - either (far or near) endpoint is removed from its host middleware;
 *     - the near middleware is gracefully killed.
 *
 * Additionally, the following failure modes can cause a channel to be closed
 * is a non-graceful manner (i.e. without sending a tear-down message):
 *     - the underlying connection to the far host fails while sending a message;
 *     - the near middleware is non-gracefully killed.
 *
 * In these situations, consensus about the state of the channel is not shared between
 * the near and far middlewares. Therefore, these closures are tracked by the near
 * middleware and relayed to the far middleware when possible, giving the final
 * condition for channel closure:
 *     - the far middleware indicates that a channel was locally closed in the past,
 *       but it was not able to perform the graceful tear-down.
 */
public class Channel extends CloseableSubject<Channel> {

    private Endpoint near;

    private RemoteEndpointDetails far;


    /**
     * Create a new channel representing the flow of data between near and far endpoints.
     *
     * @param near Reference to the endpoint object at the near end of the channel.
     * @param far Details of the far endpoint, including the host on which it resides.
     */
    public Channel(Endpoint near, RemoteEndpointDetails far) {
        this.near = near;
        this.far = far;
    }

    public Endpoint getNear() {
        return near;
    }

    public RemoteEndpointDetails getFar() {
        return far;
    }
}
