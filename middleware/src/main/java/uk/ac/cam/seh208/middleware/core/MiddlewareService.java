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
     * TODO: document.
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
     * TODO: document.
     */
    public void createEndpoint(EndpointDetails details, boolean exposed, boolean forceable)
            throws EndpointCollisionException, BadSchemaException {
        Endpoint endpoint = new Endpoint(this, details, exposed, forceable);
        if (endpointSet.add(endpoint)) {
            // Perform initialisation routines for the endpoint.
            endpoint.initialise();

            // TODO: re-register with RDC.
        } else {
            // An endpoint of this name already exists!
            throw new EndpointCollisionException(details.getName());
        }
    }

    /**
     * TODO: document.
     */
    public void destroyEndpoint(String name) throws EndpointNotFoundException {
        // Synchronise on the endpoint set to prevent interleaving endpoint
        // destruction and creation/another destruction. Note that similar
        // interleaving is not done in `createEndpoint` as the add method itself
        // is synchronised, and is the only interaction with the set in the method.
        //noinspection SynchronizeOnNonFinalField
        synchronized (endpointSet) {
            Endpoint endpoint = endpointSet.getEndpointByName(name);
            if (endpoint == null) {
                // If the returned object is null, there is no endpoint by that name.
                throw new EndpointNotFoundException(name);
            }

            // Perform destruction routines for the endpoint.
            endpoint.destroy();
            endpointSet.remove(endpoint);

            // TODO: re-register with RDC, destroy endpoint binder (if one exists).
        }
    }

    /**
     * TODO: document.
     */
    public List<String> discover(Query query) {
        // TODO: implement.
        return null;
    }

    public EndpointSet getEndpointSet() {
        // TODO: return unmodifiable view on the endpoint set.
        return endpointSet;
    }

    public Switch getCommsSwitch() {
        return commsSwitch;
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
