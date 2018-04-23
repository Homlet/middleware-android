package uk.ac.cam.seh208.middleware.core;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import java8.util.Objects;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.core.control.ControlMessageHandler;
import uk.ac.cam.seh208.middleware.core.control.Middleware;
import uk.ac.cam.seh208.middleware.core.comms.RequestSwitch;
import uk.ac.cam.seh208.middleware.core.comms.impl.ZMQSchemeConfiguration;


public class RDCService extends Service {

    /**
     * Boolean tracking whether the service has previously been started.
     */
    private boolean started;

    /**
     * Switch handling communications at the transport and network layers.
     */
    private RequestSwitch requestSwitch;

    /**
     * Read-write lock for efficient use of the database.
     */
    private ReadWriteLock lock;

    /**
     * Map of middleware instances by the endpoints known present there. Used for
     * efficient discovery of locations exposing relevant resources.
     */
    private Map<EndpointDetails, Middleware> middlewaresByEndpoint;

    /**
     * Map of endpoints by the middleware instances at which they are present. Used
     * for efficient updating and removal of entries.
     */
    private Map<Middleware, List<EndpointDetails>> endpointsByMiddleware;


    /**
     * Initialise the service fields and set up the communications switch.
     *
     * @return whether the middleware was started.
     */
    private synchronized boolean start() {
        if (started) {
            return false;
        }

        Log.i(getTag(), "RDC starting...");
        Toast.makeText(this, getText(R.string.toast_rdc_starting), Toast.LENGTH_SHORT).show();

        // Initialise object parameters.
        ControlMessageHandler handler = new ControlMessageHandler(this);
        requestSwitch = new RequestSwitch(
                Arrays.asList(
                    new ZMQSchemeConfiguration(ZMQSchemeConfiguration.DEFAULT_RDC_PORT)
                ),
                handler);

        lock = new ReentrantReadWriteLock();
        middlewaresByEndpoint = new HashMap<>();
        endpointsByMiddleware = new HashMap<>();

        Log.i(getTag(), "RDC started successfully.");
        started = true;
        return true;
    }

    /**
     * Called by Android when the service is started, either from an intent or
     * after a restart attempt.
     *
     * @return START_STICKY to indicate the OS should attempt to restart this
     *         process if it is killed to free resources.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!start()) {
            Log.d(getTag(), "Start command ignored (already started).");
        }

        // Attempt to restart this service if the scheduler kills it for resources.
        return START_STICKY;
    }

    /**
     * Called by Android when a remote process requests to bind to this service.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Attempt to start the service if not already started.
        start();

        // The RDC service has no bound interface.
        return null;
    }

    public List<Middleware> discover(Query query) {
        Log.i(getTag(), "Discovering with query \"" + query + "\"");

        // Acquire the database read lock.
        lock.readLock().lock();

        // Remove any matches limit from the query.
        Query modifiedQuery = new Query.Builder()
                .copy(query)
                .setMatches(Query.MATCH_INDEFINITELY)
                .build();

        // Find all matching locations using a stream.
        List<Middleware> middlewares = StreamSupport.stream(middlewaresByEndpoint.keySet())
                .filter(modifiedQuery.getFilter())
                .map(middlewaresByEndpoint::get)
                .distinct()
                .collect(Collectors.toList());

        // Release the database read lock.
        lock.readLock().unlock();

        return middlewares;
    }

    /**
     * Update the middleware instance record for the given location in the RDC state.
     *
     * @param middleware Middleware instance indexing the record to update.
     * @param details New list of endpoints exposed by that middleware instance.
     */
    public void update(Middleware middleware, List<EndpointDetails> details) {
        Log.i(getTag(), "Updating location " + middleware + " " +
                "with " + details.size() + " exposed endpoint(s).");

        // Acquire the database write lock.
        lock.writeLock().lock();

        // Remove the prior entry from the database.
        removeQuiet(middleware);

        // Repopulate the database with the new details.
        List<EndpointDetails> nonNullDetails = StreamSupport.stream(details)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        endpointsByMiddleware.put(middleware, nonNullDetails);
        for (EndpointDetails endpoint : nonNullDetails) {
            middlewaresByEndpoint.put(endpoint, middleware);
        }

        // Release the database write lock.
        lock.writeLock().unlock();
    }

    /**
     * Remove the middleware instance record for the given location in the RDC state.
     *
     * @param middleware Middleware instance indexing the record to remove.
     */
    public void remove(Middleware middleware) {
        Log.i(getTag(), "Removing location " + middleware);
        removeQuiet(middleware);
    }

    /**
     * Perform the remove operation without logging.
     */
    private void removeQuiet(Middleware middleware) {
        // Acquire the database write lock.
        lock.writeLock().lock();

        // Remove the endpoints list from the database.
        List<EndpointDetails> details = endpointsByMiddleware.remove(middleware);

        if (details == null) {
            // The location was not present in the map. Release the database
            // write lock and return.
            lock.writeLock().unlock();
            return;
        }

        for (EndpointDetails endpoint : details) {
            // We never put null values into endpointsByMiddleware lists in update
            // so we can skip a null check here.
            middlewaresByEndpoint.remove(endpoint);
        }

        // Release the database write lock.
        lock.writeLock().unlock();
    }

    private static String getTag() {
        return "RDC";
    }
}
