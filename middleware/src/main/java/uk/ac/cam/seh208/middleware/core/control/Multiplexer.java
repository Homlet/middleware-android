package uk.ac.cam.seh208.middleware.core.control;

import android.util.Log;
import android.util.LongSparseArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
 * the underlying connection before propagating failure to links.
 *
 * The multiplexer also provides deduplication between a number of links which share a
 * local endpoint and destination host, but different destination endpoints.
 */
public class Multiplexer extends CloseableSubject<Multiplexer> {

    /**
     * MessageStream implementor providing the underlying communications to the remote middleware.
     */
    private final MessageStream messageStream;


    /**
     * Back-reference to the owning service.
     */
    private final MiddlewareService service;

    /**
     * Indicator of whether this is a local loopback multiplexer.
     */
    private final boolean loopback;

    /**
     * Location and identifier of the remote instance of the middleware.
     */
    private final Middleware remote;

    /**
     * Map of currently open links carried by the multiplexer, indexed by their identifier.
     * This is used to determine whether a timeout closure of the multiplexer should be
     * scheduled on link closure, and to de-multiplex incoming messages between endpoints.
     */
    private final LongSparseArray<Link> links;

    /**
     * Map of currently open links carried by the multiplexer, grouped by the identifier
     * of their local endpoint. This is used to determine which link identifiers should be
     * prepended to each message for multiplexing purposes.
     */
    private final LongSparseArray<List<Link>> linksByLocalEndpoint;

    /**
     * Lambda reference to the onMessage method for registering with the message stream.
     */
    private final MessageListener listener;

    /**
     * Lock used to prevent handlers of sending and receiving from seeing partially
     * updated multiplexer state.
     */
    private final ReadWriteLock stateLock;


    Multiplexer(MiddlewareService service, Middleware remote) throws BadHostException {
        this.service = service;
        this.messageStream = service.getMessageStream(remote.getMessageLocation());
        this.remote = remote;
        links = new LongSparseArray<>();
        linksByLocalEndpoint = new LongSparseArray<>();
        stateLock = new ReentrantReadWriteLock(true);

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
     * Carry a link on this multiplexer. This requires that the link
     * remote endpoint resides on the instance of the middleware this link
     * communicates with.
     *
     * @return whether the link was successfully carried.
     */
    boolean carryLink(Link link) {
        if (isClosed()) {
            return false;
        }

        if (!Objects.equals(link.getRemote().getMiddleware(), remote)) {
            Log.e(getTag(), "Attempted to carry an invalid link.");
            return false;
        }

        // Acquire the state write lock.
        stateLock.writeLock().lock();

        // Add the link to the links map.
        links.put(link.getLinkId(), link);

        // Find the link list associated with the link's local endpoint,
        // if one such list already exists, in order to add the link to it.
        long localId = link.getLocal().getEndpointId();
        if (linksByLocalEndpoint.indexOfKey(localId) < 0) {
            // The list does not exist; create a new list and append the link.
            List<Link> linksList = new ArrayList<>();
            linksList.add(link);
            linksByLocalEndpoint.put(localId, linksList);
        } else {
            // The list exists; add the link to it.
            linksByLocalEndpoint.get(localId).add(link);
        }

        // Attempt to subscribe to link closure, dropping the link
        // when this occurs.
        if (!link.subscribeIfOpen(this::dropLink)) {
            // If the link is already closed, drop it immediately.
            dropLink(link);
            return false;
        }

        // Release the state lock.
        stateLock.writeLock().unlock();

        return true;
    }

    /**
     * Stop carrying a previously carried link. If this was the last link
     * carried by this multiplexer, set a timeout for multiplexer closure.
     *
     * @return whether the link was successfully dropped.
     */
    private boolean dropLink(Link link) {
        if (isClosed()) {
            return false;
        }

        // Acquire the state write lock.
        stateLock.writeLock().lock();

        if (links.indexOfKey(link.getLinkId()) < 0) {
            // Release the state lock.
            stateLock.writeLock().unlock();

            Log.e(getTag(), "Attempted to remove link not carried.");

            return false;
        }

        // Remove the link from the links map.
        links.remove(link.getLinkId());

        // Remove the link from the linksByLocalEndpoint list associated with
        // its local endpoint identifier.
        removeLinkByEndpoint(link, link.getLocal().getDetails());

        if (loopback) {
            // In the special case that both endpoints reside on the same middleware,
            // we must also remove the link from the linksByLocalEndpoint list
            // associated with its remote endpoint identifier.
            removeLinkByEndpoint(link, link.getRemote());
        }

        if (links.size() == 0) {
            if (BuildConfig.DEBUG && linksByLocalEndpoint.size() > 0) {
                // Release the state lock.
                stateLock.writeLock().unlock();

                throw new AssertionError("Inconsistent link state in multiplexer.");
            }

            // If all carried links are closed, set a timeout for the closure
            // of the multiplexer.
            // TODO: timeoutClose();
        }

        // Release the state lock.
        stateLock.writeLock().unlock();

        return true;
    }

    /**
     * Convenience procedure for removing a link from the linksByLocalEndpoint collection.
     * The given link will (obviously) only be removed from the list pertaining to the given
     * endpoint if the endpoint actually resides in that list.
     *
     * @param link The link to remove.
     * @param details Details of the endpoint the link is associated with.
     */
    private void removeLinkByEndpoint(Link link, EndpointDetails details) {
        long endpointId = details.getEndpointId();
        List<Link> linkList = linksByLocalEndpoint.get(endpointId);
        linkList.remove(link);
        if (linkList.isEmpty()) {
            // If the list is now empty, remove it from the map.
            linksByLocalEndpoint.remove(endpointId);
        }
    }

    /**
     * Send the message along the associated message stream, prepending the ids of all
     * carried links associated with the originator endpoint.
     */
    public void send(Endpoint local, String data) {
        if (isClosed()) {
            return;
        }

        // Acquire the state read lock.
        stateLock.readLock().lock();

        if (linksByLocalEndpoint.indexOfKey(local.getEndpointId()) < 0) {
            Log.e(getTag(), "Attempted to send a message from a local endpoint with no " +
                    "carried links.");
            return;
        }

        try {
            // Build a message efficiently using a StringBuilder object.
            StringBuilder message = new StringBuilder();
            for (Link link : linksByLocalEndpoint.get(local.getEndpointId())) {
                // For each link sharing the given local endpoint, prepend the
                // link identifier to the message.
                message.append(link.getLinkId());
                message.append("|");
            }

            if (BuildConfig.DEBUG && message.toString().isEmpty()) {
                throw new AssertionError("Bad state in linksByLocalEndpoint");
            }

            // Delimit the prefix from the message data using a second newline.
            message.append("|");
            message.append(data);

            // Send the newly built message over the associated message stream.
            messageStream.send(message.toString());
        } catch (ConnectionFailedException e) {
            close();
        }

        // Release the state lock.
        stateLock.readLock().unlock();
    }

    @Override
    public synchronized void close() {
        if (isClosed()) {
            return;
        }

        // Acquire the state write lock.
        stateLock.writeLock().lock();

        // Close (and thus drop) all remaining links.
        // Links being migrated to another multiplexer should be
        // dropped manually before closing this multiplexer, otherwise
        // they will be closed automatically here.
        int size = links.size();
        for (int i = 0; i < size; i++) {
            links.valueAt(0).close();
        }

        // Remove the message listener from the message stream.
        messageStream.unregisterListener(listener);

        // Release the state lock.
        stateLock.writeLock().unlock();

        super.close();
    }

    public Middleware getRemote() {
        return remote;
    }

    /**
     * On received raw message, split into prefix and data, and dispatch the data to
     * all local endpoints referenced by the link identifiers in the prefix.
     */
    private void onMessage(String message) {
        if (isClosed()) {
            return;
        }

        int divider = message.indexOf("||");
        String[] parts = message.substring(0, divider).split("\\|");
        String data = message.substring(divider + 2);

        // Acquire the state read lock.
        stateLock.readLock().lock();

        // Dispatch the message type and separated link identifier to the local
        // endpoint of each of the addressed links.
        for (String part : parts) {
            long linkId = Long.valueOf(part);

            if (links.indexOfKey(linkId) < 0) {
                // If the links map does not contain the link id, we
                // probably shouldn't have received it.
                // TODO: respond telling remote to close the erroneous link.
                Log.w(getTag(), "Received message for unknown link (" + linkId + ")");
                continue;
            }

            // Find the local sink endpoint associated with this link.
            Endpoint sink = links.get(linkId).getLocal();
            if (loopback) {
                // If this is a loopback multiplexer, choose the endpoint having the sink
                // polarity. This is a bit of a hack, and the state space of the multiplexer
                // would have to be changed in the future to support more exotic polarities.
                if (sink.getPolarity() != Polarity.SINK) {
                    EndpointDetails details = links.get(linkId).getRemote();
                    sink = service.getEndpointSet().getEndpointByName(details.getName());
                }
            }

            // Delegate to the message handler of the link's local endpoint.
            sink.onMessage(linkId, data);
        }

        // Release the state lock.
        stateLock.readLock().unlock();
    }

    private String getTag() {
        return "MUX[" + remote.getUUID() + "]";
    }
}
