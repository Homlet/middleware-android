package uk.ac.cam.seh208.middleware.core;

import static uk.ac.cam.seh208.middleware.common.Keys.MiddlewareService.*;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.HashMap;

import uk.ac.cam.seh208.middleware.binder.EndpointBinder;
import uk.ac.cam.seh208.middleware.binder.MiddlewareBinder;
import uk.ac.cam.seh208.middleware.common.BinderType;
import uk.ac.cam.seh208.middleware.common.EndpointCollisionException;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.EndpointNotFoundException;


public class MiddlewareService extends Service {

    /**
     * Binder object for handling general middleware IPC calls.
     */
    private MiddlewareBinder middlewareBinder;

    /**
     * Collection of binder objects for handling endpoint-specific
     * IPC calls on a particular endpoint. Indexed by endpoint name.
     */
    private HashMap<String, EndpointBinder> endpointBinders;

    /**
     * Set of endpoints currently active in the middleware.
     */
    private EndpointSet endpointSet;

    /**
     * Indicates whether it should be possible for remote instances of the
     * middleware to force commands to run on this instance.
     */
    private boolean forceable;

    /**
     * Stores the host on which the resource discovery component (RDC) is accessible.
     */
    private String rdcHost;

    /**
     * Indicates whether the middleware instance should be discoverable
     * via the registered RDC.
     */
    private boolean discoverable;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Initialise object parameters.
        endpointBinders = new HashMap<>();
        endpointSet = new EndpointSet();

        // Attempt to restart this service if the scheduler kills it for resources.
        return START_STICKY;
    }

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
                    binder = new EndpointBinder(this, name);
                    endpointBinders.put(name, binder);
                }
                return binder;

            default:
                return null;
        }
    }

    public void createEndpoint(EndpointDetails details, boolean exposed,
                               boolean forceable) throws EndpointCollisionException {
        Endpoint endpoint = new Endpoint(details, exposed, forceable);
        if (endpointSet.add(endpoint)) {
            // Perform initialisation routines for the endpoint.
            endpoint.initialise();
        } else {
            // An endpoint of this name already exists!
            throw new EndpointCollisionException(details);
        }
    }

    public void destroyEndpoint(String name) throws EndpointNotFoundException {
        // Synchronise on the endpoint set to prevent interleaving endpoint
        // destruction and creation. Note that similar interleaving is not
        // done in `createEndpoint` as the add method itself is synchronised,
        // and is the only interaction with the set in the method.
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
        }
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

    public String getRDCHost() {
        return rdcHost;
    }

    public synchronized void setRDCHost(String host) {
        this.rdcHost = host;
    }

    public boolean isDiscoverable() {
        return discoverable;
    }

    public synchronized void setDiscoverable(boolean discoverable) {
        this.discoverable = discoverable;
    }
}
