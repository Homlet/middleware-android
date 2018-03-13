package uk.ac.cam.seh208.middleware.core;

import android.app.Service;
import android.content.Intent;
import android.os.Debug;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java8.util.stream.StreamSupport;
import uk.ac.cam.seh208.middleware.binder.CombinedBinder;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.core.comms.RemoteEndpointDetails;
import uk.ac.cam.seh208.middleware.common.exception.BadHostException;
import uk.ac.cam.seh208.middleware.common.exception.EndpointCollisionException;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.exception.EndpointNotFoundException;
import uk.ac.cam.seh208.middleware.common.exception.BadSchemaException;
import uk.ac.cam.seh208.middleware.core.comms.ControlMessageHandler;
import uk.ac.cam.seh208.middleware.core.comms.Endpoint;
import uk.ac.cam.seh208.middleware.core.comms.EndpointSet;
import uk.ac.cam.seh208.middleware.core.comms.Multiplexer;
import uk.ac.cam.seh208.middleware.core.comms.MultiplexerPool;
import uk.ac.cam.seh208.middleware.core.exception.UnexpectedClosureException;
import uk.ac.cam.seh208.middleware.core.network.Address;
import uk.ac.cam.seh208.middleware.core.network.Location;
import uk.ac.cam.seh208.middleware.core.network.MessageStream;
import uk.ac.cam.seh208.middleware.core.network.RequestStream;
import uk.ac.cam.seh208.middleware.core.network.Switch;


public class MiddlewareService extends Service {

    /**
     * Boolean tracking whether the service has previously been started.
     */
    private boolean started;

    /**
     * Combined binder object for handling IPC calls.
     */
    private CombinedBinder binder;

    /**
     * Set of endpoints currently active in the middleware.
     */
    private EndpointSet endpointSet;

    /**
     * Pool of open multiplexers within the middleware.
     */
    private MultiplexerPool multiplexerPool;

    /**
     * Switch handling communications at the transport and network layers.
     */
    private Switch commsSwitch;

    /**
     * Indicates whether it should be possible for remote instances of the
     * middleware to force commands to run on this instance.
     */
    private boolean forceable;

    /**
     * Stores the address of the remote host on which the resource discovery component
     * (RDC) is accessible.
     */
    private Address rdcAddress;

    /**
     * Indicates whether the middleware instance should be discoverable
     * via the registered RDC.
     */
    private boolean discoverable;


    /**
     * Called by Android when the service is started, either from an intent or
     * after a restart attempt.
     *
     * @return START_STICKY to indicate the OS should attempt to restart this
     *         process if it is killed to free resources.
     */
    @Override
    public synchronized int onStartCommand(Intent intent, int flags, int startId) {
        if (started) {
            Log.i(getTag(), "Start command ignored (already started).");
            return START_STICKY;
        }

        Log.i(getTag(), "Middleware starting...");
        Toast.makeText(this, getText(R.string.toast_starting), Toast.LENGTH_SHORT).show();

        // Initialise object parameters.
        binder = new CombinedBinder(this);
        endpointSet = new EndpointSet();
        multiplexerPool = new MultiplexerPool(this);

        ControlMessageHandler handler = new ControlMessageHandler(this);
        commsSwitch = new Switch(Arrays.asList(Switch.SCHEME_ZMQ), handler);

        Log.i(getTag(), "Middleware started successfully.");
        started = true;

        // Attempt to restart this service if the scheduler kills it for resources.
        return START_STICKY;
    }

    /**
     * Called by Android when a remote process requests to bind to this service.
     */
    @Nullable
    @Override
    public synchronized IBinder onBind(Intent intent) {
        Log.i(getTag(), "Client bound to service.");
        return binder;
    }

    /**
     * Create a new endpoint within the middleware, having the given details and settings.
     * The endpoint will have a freshly generated endpoint identifier, and is initialised
     * after creation.
     */
    public void createEndpoint(EndpointDetails details, boolean exposed, boolean forceable)
            throws EndpointCollisionException, BadSchemaException {
        Endpoint endpoint = new Endpoint(this, details, exposed, forceable);

        // Synchronise on the endpoint set to prevent interleaving endpoint
        // destruction and creation/another destruction.
        //noinspection SynchronizeOnNonFinalField
        synchronized (endpointSet) {
            Log.i(getTag(), "Creating endpoint " + details);

            if (endpointSet.getEndpointByName(details.getName()) != null) {
                // An endpoint of this name already exists!
                Log.w(getTag(), "Endpoint with name \"" + details.getName() + "\" already exists.");
                throw new EndpointCollisionException(details.getName());
            }

            // Perform initialisation routines for the endpoint.
            endpoint.initialise();

            // Add the endpoint to the set.
            endpointSet.add(endpoint);

            Log.i(getTag(), "Endpoint " + details + " created.");
        }

        // TODO: schedule update message to the RDC.
    }

    /**
     * Destroy an existing endpoint and all associated mappings, closing any open
     * channels having this endpoint at either end. This frees the endpoint name
     * for use in newly created endpoints; however, the endpoint identifier will
     * never be reused (modulo collisions).
     */
    public void destroyEndpoint(String name) throws EndpointNotFoundException {
        // Synchronise on the endpoint set to prevent interleaving endpoint
        // destruction and creation/another destruction.
        //noinspection SynchronizeOnNonFinalField
        synchronized (endpointSet) {
            Endpoint endpoint = endpointSet.getEndpointByName(name);
            if (endpoint == null) {
                // If the returned object is null, there is no endpoint by that name.
                throw new EndpointNotFoundException(name);
            }

            // Perform destruction routines for the endpoint and binder.
            binder.invalidateEndpoint(name);
            endpoint.destroy();
            endpointSet.remove(endpoint);

            Log.i(getTag(), "Endpoint with name \"" + name + "\" destroyed.");
        }

        // TODO: schedule update message to the RDC.
    }

    /**
     * Return the unique pooled multiplexer for the given remote location.
     *
     * @param remote Remote location with which the multiplexer communicates.
     *
     * @return a reference to a Multiplexer object.
     *
     * @throws BadHostException if it was impossible to create an underlying message
     *                          stream to the given host.
     */
    public Multiplexer getMultiplexer(Location remote) throws BadHostException {
        return multiplexerPool.getMultiplexer(remote);
    }

    /**
     * Return a message stream to the given location, preferring certain
     * network schemes according to the set policy.
     *
     * @param remote Location of the remote host to get a message stream to.
     *
     * @return a reference to a message stream.
     *
     * @throws BadHostException if it was impossible to create a message stream to the
     *                          given host.
     */
    public MessageStream getMessageStream(Location remote) throws BadHostException {
        // TODO: implement.
        return null;
    }

    /**
     * Return a request stream to the given location, preferring certain
     * network schemes according to the set policy.
     *
     * @param remote Location of the remote host to get a request stream to.
     *
     * @return a reference to a request stream.
     *
     * @throws BadHostException if it was impossible to create a message stream to the
     *                          given host.
     */
    public RequestStream getRequestStream(Location remote) throws BadHostException {
        // TODO: implement.
        return null;
    }

    /**
     * Send a QUERY_COARSE control message to the registered RDC, returning the resultant
     * list of remote locations. If no locations match the query, an empty list is returned;
     * if something went wrong contacting the RDC, null is returned.
     *
     * @return a list of locations exposing endpoints which match the query, or null if
     *         the RDC could not be contacted.
     */
    public List<Location> discover(Query query) {
        Log.i(getTag(), "Querying RDC for peers matching " + query);

        // TODO: implement.
        return null;
    }

    /**
     * Filter existing endpoints by a query, and open channels from all matching
     * to a single given remote endpoint.
     *
     * @param query Query with which to filter local endpoints.
     * @param remote Remote endpoint to which to open channels.
     *
     * @return a list of endpoint-details for the opened channels.
     */
    public List<RemoteEndpointDetails> openChannels(Query query, RemoteEndpointDetails remote) {
        Log.i(getTag(), "Opening channels to " + remote +
                " from local endpoints matching " + query);

        // Keep track of endpoints returned.
        List<RemoteEndpointDetails> endpoints = new ArrayList<>();

        //noinspection SynchronizeOnNonFinalField
        synchronized (endpointSet) {
            // Filter the endpoint set according to the query.
            StreamSupport.stream(endpointSet)
                    .filter(e -> query.getFilter().test(e.getRemoteDetails()))
                    .forEach(e -> {
                        try {
                            e.openChannel(remote);
                            endpoints.add(e.getRemoteDetails());
                        } catch (BadHostException | UnexpectedClosureException ex) {
                            Log.w(getTag(), "Error opening channel on endpoint (" +
                                    e.getEndpointId() + ")");
                        }
                    });

            // Return the filtered channel details.
            return endpoints;
        }
    }

    public EndpointSet getEndpointSet() {
        // TODO: return unmodifiable view on the endpoint set.
        return endpointSet;
    }

    public Location getLocation() {
        return commsSwitch.getLocation();
    }

    public boolean isForceable() {
        return forceable;
    }

    public synchronized void setForceable(boolean forceable) {
        Log.i(getTag(), "Middleware forceable flag set " + forceable);
        this.forceable = forceable;
    }

    public Address getRDCAddress() {
        return rdcAddress;
    }

    public synchronized void setRDCAddress(Address address) {
        Log.i(getTag(), "RDC address set to " + address);
        this.rdcAddress = address;
    }

    public boolean isDiscoverable() {
        return discoverable;
    }

    public synchronized void setDiscoverable(boolean discoverable) {
        Log.i(getTag(), "Middleware discoverable flag set " + discoverable);
        this.discoverable = discoverable;
    }

    private String getTag() {
        return "MW";
    }
}
