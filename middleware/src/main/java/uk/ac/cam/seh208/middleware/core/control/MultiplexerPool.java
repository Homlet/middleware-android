package uk.ac.cam.seh208.middleware.core.control;

import android.util.LongSparseArray;

import uk.ac.cam.seh208.middleware.common.exception.BadHostException;
import uk.ac.cam.seh208.middleware.core.MiddlewareService;


/**
 * Class responsible for creating and memoising multiplexers for message streams.
 */
public class MultiplexerPool {

    /**
     * Back-reference to the owning instance of the middleware service.
     */
    private MiddlewareService service;

    /**
     * Map of multiplexers indexed by the unique identifier of their remote location.
     */
    private LongSparseArray<Multiplexer> multiplexers;


    public MultiplexerPool(MiddlewareService service) {
        this.service = service;

        multiplexers = new LongSparseArray<>();
    }

    /**
     * Get a multiplexer to a given remote location, constructing a new one if none
     * currently exist for the remote.
     *
     * @param remote Remote middleware instance with which the multiplexer
     *               should exchange messages.
     *
     * @return a reference to a Multiplexer object.
     */
    public synchronized Multiplexer getMultiplexer(Middleware remote) throws BadHostException {
        if (multiplexers.indexOfKey(remote.getUUID()) >= 0) {
            // If a multiplexer already exists in the map for this remote location,
            // return this instead of constructing a new one.
            return multiplexers.get(remote.getUUID());
        }

        // Construct a new multiplexer to the remote host, and put it in the map.
        Multiplexer multiplexer = new Multiplexer(service, remote);
        multiplexers.put(remote.getUUID(), multiplexer);

        // Attempt to subscribe to multiplexer closure, removing the multiplexer from
        // map when this occurs.
        if (!multiplexer.subscribeIfOpen(this::removeMultiplexer)) {
            // If the multiplexer is already closed, remove it from the map immediately.
            removeMultiplexer(multiplexer);

            // Recursively re-attempt to open a multiplexer.
            // TODO: iterate instead (not a big deal as this shouldn't be a hot path).
            return getMultiplexer(remote);
        }

        return multiplexer;
    }

    private synchronized void removeMultiplexer(Multiplexer multiplexer) {
        multiplexers.remove(multiplexer.getRemote().getUUID());
    }
}
