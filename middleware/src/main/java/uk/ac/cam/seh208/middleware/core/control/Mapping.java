package uk.ac.cam.seh208.middleware.core.control;

import android.util.LongSparseArray;

import java.util.List;
import java.util.Random;

import java8.util.stream.StreamSupport;
import uk.ac.cam.seh208.middleware.common.Persistence;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.common.exception.BadHostException;
import uk.ac.cam.seh208.middleware.core.CloseableSubject;


/**
 * Object storing the result of a mapping command called on an endpoint.
 *
 * Consists of a collections of links from the local endpoint
 * to remote endpoints.
 *
 * The persistence level determines how the mapping handles failure; partial
 * failure is the closure of some links within the mapping, while complete
 * failure is the closure of all links, or the local middleware instance
 * being killed by the scheduler.
 */
public class Mapping extends CloseableSubject<Mapping> {

    /**
     * Universally unique identifier for the link.
     */
    private long mappingId;

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
     * Map of links constituent to the mapping. This is used to determine how many
     * new links should be established to restore the mapping.
     */
    private LongSparseArray<Link> links;

    /**
     * Original number of links constituent to the mapping.
     */
    private int capacity;


    /**
     * Construct a new mapping object, subscribing to every link from the given
     * list as an open link.
     *
     * @param query Query object to be stored for restoration.
     * @param persistence Persistence level determining restoration strategy.
     * @param links List of links which should be included as part of the mapping.
     */
    Mapping(Endpoint local, Query query, Persistence persistence, List<Link> links) {
        this.mappingId = new Random(System.nanoTime()).nextLong();
        this.local = local;
        this.query = query;
        this.persistence = persistence;
        this.links = new LongSparseArray<>();

        // Add each link as an open constituent of the mapping.
        if (links == null) {
            return;
        }
        StreamSupport.stream(links).forEach(this::addLink);

        capacity = this.links.size();
    }

    /**
     * Add a link to the mapping, subscribing to its closure for
     * restoration purposes.
     *
     * @param link Reference to the link to add.
     */
    private synchronized void addLink(Link link) {
        if (link == null) {
            // We cannot subscribe to a null object.
            return;
        }

        // Put the link in the open links list.
        links.put(link.getLinkId(), link);

        // Attempt to subscribe to the link. If the link was closed
        // prior to this point, continue with no action.
        if (!link.subscribeIfOpen(this::onLinkClose)) {
            links.remove(link.getLinkId());
        }
    }

    @Override
    public void close() {
        // Drop the endpoint reference to speed up garbage collection.
        local = null;

        // Close all remaining links.
        int size = links.size();
        for (int i = 0; i < size; i++) {
            links.valueAt(0).close();
        }

        super.close();
    }

    public String getEndpointName() {
        return local.getName();
    }

    public long getMappingId() {
        return mappingId;
    }

    public Query getQuery() {
        return query;
    }

    public Persistence getPersistence() {
        return persistence;
    }


    /**
     * Called by observed links on closure.
     *
     * Mapping restoration logic is rooted here, meaning on link closure
     * persistent mappings are immediately restored by the middleware.
     *
     * @param link Reference to the link which closed.
     */
    private synchronized void onLinkClose(Link link) {
        links.remove(link.getLinkId());

        try {
            // Attempt to restore links; if
            restore();
        } catch (BadHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * Implementation of mapping restoration logic.
     *
     * @throws BadHostException when the RDC is unreachable.
     */
    private void restore() throws BadHostException {
        // Switch restoration strategy depending on persistence level.
        switch (persistence) {
            case NONE:
                // Do nothing.
                return;

            case RESEND_QUERY:
                if (links.size() == 0) {
                    // If there are no links remaining, restore the mapping
                    // by re-sending the query.
                    List<Middleware> remotes = local.getService().discover(query);
                    List<Link> establishedLinks = local.establishLinks(remotes, query);
                    for (Link link : establishedLinks) {
                        addLink(link);
                    }
                }
                return;

            case RESEND_QUERY_INDIVIDUAL:
                // Send a modified query to restore the remaining links.
                Query modifiedQuery = new Query.Builder()
                        .copy(query)
                        .setMatches(capacity - links.size())
                        .build();
                List<Middleware> remotes = local.getService().discover(query);
                List<Link> establishedLinks =
                        local.establishLinks(remotes, modifiedQuery);
                for (Link link : establishedLinks) {
                    addLink(link);
                }
                return;

            default:
                // TODO: support for EXACT persistence (stretch goal).
        }
    }
}
