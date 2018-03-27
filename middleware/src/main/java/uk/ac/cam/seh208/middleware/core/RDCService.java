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
import java.util.concurrent.locks.ReentrantReadWriteLock;

import java8.util.Objects;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.core.comms.ControlMessageHandler;
import uk.ac.cam.seh208.middleware.core.network.Location;
import uk.ac.cam.seh208.middleware.core.network.RequestSwitch;
import uk.ac.cam.seh208.middleware.core.network.impl.ZMQSchemeConfiguration;


public class RDCService extends Service {

    /**
     * Boolean tracking whether the service has previously been started.
     */
    private boolean started;

    /**
     * Switch handling communications at the transport and network layers.
     */
    private RequestSwitch requestSwitch;

    // TODO: use persistent backing storage of resources.
    /**
     * Read-write lock for efficient use of the database.
     */
    private ReentrantReadWriteLock lock;

    /**
     * Map of locations by the endpoints known present there. Used for
     * efficient discovery of locations exposing relevant resources.
     */
    private Map<EndpointDetails, Location> locationsByEndpoint;

    /**
     * Map of endpoints by the locations at which they are present. Used
     * for efficient updating and removal of entries.
     */
    private Map<Location, List<EndpointDetails>> endpointsByLocation;


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
        locationsByEndpoint = new HashMap<>();
        endpointsByLocation = new HashMap<>();

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

    public List<Location> discover(Query query) {
        Log.i(getTag(), "Discovering with query \"" + query + "\"");

        synchronized (lock.readLock()) {
            // Remove any matches limit from the query.
            Query modifiedQuery = new Query.Builder()
                    .copy(query)
                    .setMatches(Query.MATCH_INDEFINITELY)
                    .build();

            // Find all matching locations using a stream.
            return StreamSupport.stream(locationsByEndpoint.keySet())
                    .filter(modifiedQuery.getFilter())
                    .map(locationsByEndpoint::get)
                    .distinct()
                    .collect(Collectors.toList());
        }
    }

    /**
     * Update the middleware instance record for the given location in the RDC state.
     *
     * @param location Location indexing the middleware instance record.
     * @param details New list of endpoints exposed by that middleware instance.
     */
    public void update(Location location, List<EndpointDetails> details) {
        Log.i(getTag(), "Updating location " + location + " " +
                "with " + details.size() + "exposed endpoints.");

        synchronized (lock.writeLock()) {
            // Remove the prior entry from the database.
            removeQuiet(location);

            // Repopulate the database with the new details.
            List<EndpointDetails> nonNullDetails = StreamSupport.stream(details)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            endpointsByLocation.put(location, nonNullDetails);
            for (EndpointDetails endpoint : nonNullDetails) {
                locationsByEndpoint.put(endpoint, location);
            }
        }
    }

    /**
     * Remove the middleware instance record for the given location in the RDC state.
     *
     * @param location Location indexing the middleware instance record to remove.
     */
    public void remove(Location location) {
        Log.i(getTag(), "Removing location " + location);
        removeQuiet(location);
    }

    /**
     * Perform the remove operation without logging.
     */
    private void removeQuiet(Location location) {
        synchronized (lock.writeLock()) {
            // Remove the endpoints list from the database.
            List<EndpointDetails> details = endpointsByLocation.remove(location);

            if (details == null) {
                // The location was not present in the map.
                return;
            }

            for (EndpointDetails endpoint : details) {
                // We never put null values into endpointsByLocation lists in update
                // so we can skip a null check here.
                locationsByEndpoint.remove(endpoint);
            }
        }
    }

    private static String getTag() {
        return "RDC";
    }
}
