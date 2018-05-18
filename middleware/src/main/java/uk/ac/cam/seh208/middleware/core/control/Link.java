package uk.ac.cam.seh208.middleware.core.control;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.core.CloseableSubject;


/**
 * An active channel for data to flow from one local endpoint to
 * another (remote), usually residing on a remote instance of the middleware.
 *
 * Link objects will always exist in pairs, like the sockets of a TCP stream.
 *
 * Links do not actually implement the means for data transfer; this is
 * handled by the Multiplexer class. However, connection objects reference
 * links in order to implement multiplexing.
 *
 * Once closed, a link cannot be re-opened. Gracefully closing a link sends
 * a tear-down control message to the remote middleware, so consensus is maintained
 * about the state of the link. Links are closed in the following situations:
 *     - (for links maintained by a mapping) the link's mapping is removed;
 *     - (for links not maintained by a mapping) the endpoint closes all associated links;
 *     - either (remote or local) endpoint is removed from its host middleware;
 *     - the local middleware is gracefully killed.
 *
 * Additionally, the following failure modes can cause a link to be closed
 * is a non-graceful manner (i.e. without sending a tear-down message):
 *     - the underlying connection to the remote host fails while sending a message;
 *     - the local middleware is non-gracefully killed.
 *
 * In these situations, consensus about the state of the link is not shared between
 * the local and remote middlewares. Therefore, these closures are tracked by the local
 * middleware and relayed to the remote middleware when possible, giving the final
 * condition for link closure:
 *     - the remote middleware indicates that a link was locally closed in the past,
 *       but it was not able to perform the graceful tear-down.
 */
public class Link extends CloseableSubject<Link> {

    /**
     * Generate a deterministic identifier based on the endpoints of the link. This
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
     * Universally unique identifier for the link.
     */
    private long linkId;

    /**
     * Reference to the endpoint at the local end of the link.
     */
    private Endpoint local;

    /**
     * Details of the endpoint at the remote end of the link.
     */
    private RemoteEndpointDetails remote;


    /**
     * Create a new link representing the flow of data between local and remote endpoints.
     *
     * @param local Reference to the endpoint object at the local end of the link.
     * @param remote Details of the remote endpoint, including the host on which it resides.
     */
    Link(Endpoint local, RemoteEndpointDetails remote) {
        linkId = generateId(local.getDetails(), remote);
        this.local = local;
        this.remote = remote;
    }

    public long getLinkId() {
        return linkId;
    }

    public Endpoint getLocal() {
        return local;
    }

    public RemoteEndpointDetails getRemote() {
        return remote;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Link other = (Link) obj;

        return linkId == other.linkId;
    }
}
