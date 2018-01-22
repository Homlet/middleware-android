package uk.ac.cam.seh208.middleware.core;

import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.seh208.middleware.common.RemoteEndpointDetails;


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
public abstract class Channel {

    private Endpoint near;

    private RemoteEndpointDetails far;

    private List<ChannelObserver> observers;

    private boolean open;  // TODO: change to state enum.


    /**
     * Create a new channel representing the flow of data between near and far endpoints.
     *
     * @param near Reference to the endpoint object at the near end of the channel.
     * @param far Details of the far endpoint, including the host on which it resides.
     */
    public Channel(Endpoint near, RemoteEndpointDetails far) {
        this.near = near;
        this.far = far;
        this.observers = new ArrayList<>();
        open = true;
    }

    /**
     * Subscribe a ChannelObserver to channel events.
     */
    public synchronized void subscribe(ChannelObserver observer) {
        observers.add(observer);
    }

    /**
     * Atomically subscribe a ChannelObserver to channel events only if the
     * channel is currently open.
     *
     * @return whether subscription took place.
     */
    public synchronized boolean subscribeIfOpen(ChannelObserver observer) {
        if (open) {
            subscribe(observer);
            return true;
        }

        return false;
    }

    /**
     * Permanently close the channel, notifying the change to observers.
     */
    public synchronized void close() {
        if (!open) {
            return;
        }

        // Notify all observers of the channel closure.
        for (ChannelObserver observer : observers) {
            observer.onClose(this);
        }

        // Allow observers and channels to be efficiently garbage collected by
        // explicitly closing the reference loop now no more events can be observed.
        observers.clear();

        open = false;
    }

    public Endpoint getNear() {
        return near;
    }

    public RemoteEndpointDetails getFar() {
        return far;
    }

    public boolean isOpen() {
        return open;
    }
}
