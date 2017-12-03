package uk.ac.cam.seh208.middleware.binder;

import android.os.RemoteException;

import java.util.List;

import uk.ac.cam.seh208.middleware.common.EndpointCommand;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.MiddlewareCommand;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.core.EndpointCollisionException;
import uk.ac.cam.seh208.middleware.core.EndpointNotFoundException;
import uk.ac.cam.seh208.middleware.core.MiddlewareService;


// TODO: differentiate RemoteException types by class.
/**
 * Implementation of the middleware inter-process interface stub.
 *
 * This is defined in a thread-safe manner, as IPC procedure calls
 * are dispatched asynchronously from a thread pool maintained by
 * the Android runtime.
 *
 * The interface is described in IMiddleware.aidl
 *
 * @see IMiddleware
 */
public class MiddlewareBinder extends IMiddleware.Stub {
    /**
     * Instance of the middleware service this binder is exposing.
     */
    private final MiddlewareService service;


    public MiddlewareBinder(MiddlewareService service) {
        this.service = service;
    }

    /**
     * Create a new endpoint with the given specification. Upon creation,
     * the endpoint is active and can begin mapping and (if exposed) accepting
     * mappings from remote hosts.
     *
     * @param details Immutable details to use for the endpoint.
     * @param exposed Whether the endpoint should be considered for mapping when
     *                the middleware receives a remote mapping query.
     * @param forceable Whether it should be possible for other instances of the
     *                  middleware to run commands remotely on the endpoint.
     *
     * @throws RemoteException when the given name for the endpoint is already
     *                         assigned to an active endpoint.
     */
    @Override
    public void createEndpoint(EndpointDetails details, boolean exposed,
                               boolean forceable) throws RemoteException {
        try {
            service.createEndpoint(details, exposed, forceable);
        } catch (EndpointCollisionException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    /**
     * Destroy an existing endpoint, given its unique name. Upon destruction,
     * all endpoint mappings are torn down, and the name becomes available for
     * use by another endpoint.
     *
     * @param name Unique name of the endpoint to destroy.
     *
     * @throws RemoteException when no endpoint exists with the given name.
     */
    @Override
    public void destroyEndpoint(String name) throws RemoteException {
        try {
            service.destroyEndpoint(name);
        } catch(EndpointNotFoundException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    /**
     * List the details of all the currently active endpoint in the middleware.
     *
     * @return a list of EndpointDetails objects.
     */
    @Override
    public List<EndpointDetails> getAllEndpointDetails() {
        // TODO: implement.
        return null;
    }

    /**
     * Get the details for a specific endpoint, from its unique name.
     *
     * @param name Unique name of the endpoint.
     *
     * @return an EndpointDetails object.
     *
     * @throws RemoteException when no endpoint exists with the given name.
     */
    @Override
    public EndpointDetails getEndpointDetails(String name) throws RemoteException {
        // TODO: implement.
        return null;
    }

    /**
     * Run a general middleware command on a remote instance of the middleware.
     *
     * @param host The hostname of the device the middleware is accessible on.
     * @param command Object describing the command.
     *
     * @throws RemoteException if the hostname is an invalid URL.
     */
    @Override
    public void force(String host, MiddlewareCommand command) throws RemoteException {
        // TODO: implement.
    }

    /**
     * Run a command on a remote endpoint (an endpoint of a remote middleware instance).
     *
     * @param host The hostname of the device the middleware is accessible on.
     * @param name The name of the endpoint to run the command on.
     * @param command Object describing the command.
     *
     * @throws RemoteException if the hostname is an invalid URL.
     */
    @Override
    public void forceEndpoint(String host, String name,
                              EndpointCommand command) throws RemoteException {
        // TODO: implement.
    }

    /**
     * Specify whether it should be possible for remote instances of the middleware
     * to force commands to run on this instance.
     *
     * NOTE: when false, this value overrides any values set for individual endpoints.
     *
     * @param forceable Whether this middleware instance accepts remote commands.
     */
    @Override
    public void setForceable(boolean forceable) {
        // TODO: implement.
    }

    /**
     * Set the hostname on which the resource discovery component (RDC) is accessible.
     *
     * @param host The hostname of the device the RDC is accessible on.
     *
     * @throws RemoteException if the hostname is an invalid URL.
     */
    @Override
    public void setRDCHost(String host) throws RemoteException {
        // TODO: implement.
    }

    /**
     * Set whether the middleware instance should be discoverable via the registered
     * RDC. When the middleware is non-discoverable, the RDC will not return the
     * middleware hostname as a response to queries from other middleware instances.
     *
     * @param discoverable Whether the middleware should be discoverable.
     */
    @Override
    public void setDiscoverable(boolean discoverable) throws RemoteException {
        // TODO: implement.
    }

    /**
     * Send a new resource discovery query to the registered RDC. The result is a
     * list of hostnames running middleware instances which expose endpoints
     * matching the query.
     *
     * Note that for the purposes of resource discovery, the 'matches' field of the
     * query, which limits the number of endpoint matches per filter, is ignored.
     *
     * @param query Query to send to the RDC. Note that the matches field is ignored.
     *
     * @return a list of hostnames corresponding to middleware instances.
     *
     * @throws RemoteException when no RDC hostname is set.
     */
    @Override
    public List<String> discover(Query query) throws RemoteException {
        // TODO: implement.
        return null;
    }
}
