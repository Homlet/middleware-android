package uk.ac.cam.seh208.middleware.core;

import java.net.InetAddress;
import java.net.UnknownHostException;

import uk.ac.cam.seh208.middleware.common.RemoteEndpointDetails;
import uk.ac.cam.seh208.middleware.common.exception.BadHostException;

/**
 * An active channel for data to flow from one local endpoint (near) to
 * another (far), usually residing on a remote instance of the middleware.
 *
 * Channels will always exist in pairs, like socket endpoints.
 *
 * Where the far end of a channel is remote, the channel internally uses
 * a JeroMQ socket in TCP mode to direct the traffic. Otherwise, a local
 * message queue is used.
 *
 * Once closed, a channel cannot be re-opened. Channels are closed in the
 * following situations:
 *     - either (far or near) endpoint is removed from its host middleware;
 *     - the internal socket times out (where the far endpoint is remote);
 *     - the near host (local) middleware is killed.
 */
public abstract class Channel {

    /**
     * Factory method for creating channel objects.
     *
     * TODO: determine reasonable strategy for sharing pub/sub JeroMQ sockets between channels.
     *
     * @param near Reference to the endpoint object at the near end of the channel.
     * @param far Details of the far endpoint, including the host on which it resides.
     *
     * @return A newly constructed object of the relevant Channel subclass.
     *
     * @throws BadHostException when the host of the far endpoint is unresolvable.
     */
    public static Channel makeChannel(Endpoint near, RemoteEndpointDetails far, boolean initiating)
            throws BadHostException {
        if (!initiating) {
            // TODO: wait for initiator to create connection, and collect channel from the pool.
            return null;
        }

        try {
            if (InetAddress.getByName(far.getHost()).isAnyLocalAddress()) {
                // TODO: local loopback implementation.
                return null;
            }
        } catch (UnknownHostException e) {
            // Rethrow as BadHostException so the exception may be propagated through Android IPC.
            throw new BadHostException(far.getHost());
        }

        // TODO: JeroMQ socket implementation.
        return null;
    }


    /**
     * Permanently close the channel.
     */
    public void close() {
        // TODO: implement.
    }
}
