package uk.ac.cam.seh208.middleware.core;

import static uk.ac.cam.seh208.middleware.common.Keys.MiddlewareService.*;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.ArrayMap;

import java.util.Arrays;
import java.util.List;

import uk.ac.cam.seh208.middleware.binder.EndpointBinder;
import uk.ac.cam.seh208.middleware.binder.MiddlewareBinder;
import uk.ac.cam.seh208.middleware.common.BinderType;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.common.exception.EndpointCollisionException;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.exception.EndpointNotFoundException;
import uk.ac.cam.seh208.middleware.common.exception.BadSchemaException;
import uk.ac.cam.seh208.middleware.core.comms.Endpoint;
import uk.ac.cam.seh208.middleware.core.comms.EndpointSet;
import uk.ac.cam.seh208.middleware.core.network.Address;
import uk.ac.cam.seh208.middleware.core.network.Location;
import uk.ac.cam.seh208.middleware.core.network.impl.Switch;


public class MiddlewareService extends Service {

    /**
     * Binder object for handling general middleware IPC calls.
     */
    private MiddlewareBinder middlewareBinder;

    /**
     * Collection of binder objects for handling endpoint-specific
     * IPC calls on a particular endpoint. Indexed by endpoint name.
     */
    private ArrayMap<String, EndpointBinder> endpointBinders;

    /**
     * Set of endpoints currently active in the middleware.
     */
    private EndpointSet endpointSet;

    /**
     * Pool of open connections within the middleware.
     */
//    private ConnectionPool connectionPool;

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
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Initialise object parameters.
        endpointBinders = new ArrayMap<>();
        endpointSet = new EndpointSet();
        commsSwitch = new Switch(Arrays.asList(Switch.SCHEME_ZMQ));

        // Attempt to restart this service if the scheduler kills it for resources.
        return START_STICKY;
    }

    /**
     * Called by Android when a remote process requests to bind to this service. The
     * intent is used to determine whether the process wants a general middleware API
     * binder, or a binder exposing the API of a particular endpoint.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Determine which type of binder is being requested.
        BinderType binderType = (BinderType) intent.getSerializableExtra(BINDER_TYPE);

        switch (binderType) {
            case MIDDLEWARE:
                // Return a middleware binder for servicing general commands.
                if (middlewareBinder == null) {
                    // Construct a new binder if one does not yet exist.
                    middlewareBinder = new MiddlewareBinder(this);
                }
                return middlewareBinder;

            case ENDPOINT:
                // Return an endpoint binder for servicing a particular endpoint.
                String name = intent.getStringExtra(ENDPOINT_NAME);
                EndpointBinder binder = endpointBinders.get(name);
                if (binder == null) {
                    // Construct a new binder if one does not already exist for this endpoint.
                    binder = new EndpointBinder(endpointSet.getEndpointByName(name));
                    endpointBinders.put(name, binder);
                }
                return binder;

            default:
                return null;
        }
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
            if (endpointSet.getEndpointByName(details.getName()) != null) {
                // An endpoint of this name already exists!
                throw new EndpointCollisionException(details.getName());
            }

            // Perform initialisation routines for the endpoint.
            endpoint.initialise();

            // Add the endpoint to the set.
            endpointSet.add(endpoint);
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

            // TODO: destroy endpoint binder (if one exists).

            // Perform destruction routines for the endpoint.
            endpoint.destroy();
            endpointSet.remove(endpoint);
        }

        // TODO: schedule update message to the RDC.
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
        // TODO: implement.
        return null;
    }

    public EndpointSet getEndpointSet() {
        // TODO: return unmodifiable view on the endpoint set.
        return endpointSet;
    }

    public boolean isForceable() {
        return forceable;
    }

    public synchronized void setForceable(boolean forceable) {
        this.forceable = forceable;
    }

    public Address getRDCAddress() {
        return rdcAddress;
    }

    public synchronized void setRDCAddress(Address address) {
        this.rdcAddress = address;
    }

    public boolean isDiscoverable() {
        return discoverable;
    }

    public synchronized void setDiscoverable(boolean discoverable) {
        this.discoverable = discoverable;
    }
}
