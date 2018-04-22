package uk.ac.cam.seh208.middleware.binder;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.seh208.middleware.common.exception.BadHostException;
import uk.ac.cam.seh208.middleware.common.EndpointCommand;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.MiddlewareCommand;
import uk.ac.cam.seh208.middleware.common.exception.BadSchemaException;
import uk.ac.cam.seh208.middleware.common.exception.EndpointCollisionException;
import uk.ac.cam.seh208.middleware.common.exception.EndpointNotFoundException;
import uk.ac.cam.seh208.middleware.core.control.Endpoint;
import uk.ac.cam.seh208.middleware.core.MiddlewareService;
import uk.ac.cam.seh208.middleware.core.exception.MalformedAddressException;
import uk.ac.cam.seh208.middleware.core.comms.Address;
import uk.ac.cam.seh208.middleware.core.comms.Location;


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


    MiddlewareBinder(MiddlewareService service) {
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
     * @throws EndpointCollisionException when the given name for the endpoint is
     *                                    already assigned to an active endpoint.
     */
    @Override
    public void createEndpoint(EndpointDetails details, boolean exposed, boolean forceable)
            throws EndpointCollisionException, BadSchemaException {
        // Strip the generated id from the endpoint details object (this is a bit of a bodge).
        // This is done to prevent malicious clients trying to create endpoints with duplicate
        // endpoint ids. A proper solution would use different objects to serialise endpoint
        // details to clients and peers.
        EndpointDetails safeDetails = new EndpointDetails(
                details.getName(),
                details.getDesc(),
                details.getPolarity(),
                details.getSchema(),
                details.getTags());

        service.createEndpoint(safeDetails, exposed, forceable);
    }

    /**
     * Destroy an existing endpoint, given its unique name. Upon destruction,
     * all endpoint mappings are torn down, and the name becomes available for
     * use by another endpoint.
     *
     * @param name Unique name of the endpoint to destroy.
     *
     * @throws EndpointNotFoundException when no endpoint exists with the given name.
     */
    @Override
    public void destroyEndpoint(String name) throws EndpointNotFoundException {
        service.destroyEndpoint(name);
    }

    /**
     * Get the details for a specific endpoint, from its unique name.
     *
     * @param name Unique name of the endpoint.
     *
     * @return an EndpointDetails object.
     *
     * @throws EndpointNotFoundException when no endpoint exists with the given name.
     */
    @Override
    public EndpointDetails getEndpointDetails(String name) throws EndpointNotFoundException {
        Endpoint endpoint = service.getEndpointSet().getEndpointByName(name);
        if (endpoint == null) {
            throw new EndpointNotFoundException(name);
        }
        return endpoint.getDetails();
    }

    /**
     * List the details of all the currently active endpoint in the middleware.
     *
     * Endpoints are listed in no particular order.
     *
     * @return a list of EndpointDetails objects.
     */
    @Override
    public List<EndpointDetails> getAllEndpointDetails() {
        ArrayList<EndpointDetails> details = new ArrayList<>();
        for (Endpoint endpoint : service.getEndpointSet()) {
            details.add(endpoint.getDetails());
        }
        return details;
    }

    /**
     * Run a general middleware command on a remote instance of the middleware.
     *
     * @param uuid UUID of the middleware to force.
     * @param command Object describing the command.
     *
     * @throws BadHostException if the given host is invalid.
     */
    @Override
    public void force(long uuid, MiddlewareCommand command) throws BadHostException {
        // TODO: implement.
    }

    /**
     * Run a command on a remote endpoint (an endpoint of a remote middleware instance).
     *
     * @param uuid UUID of the middleware on which the endpoint resides.
     * @param name The name of the endpoint to run the command on.
     * @param command Object describing the command.
     *
     * @throws BadHostException if the given host is invalid.
     */
    @Override
    public void forceEndpoint(long uuid, String name,
                              EndpointCommand command) throws BadHostException {
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
        service.setForceable(forceable);
    }

    /**
     * Set the host on which the resource discovery component (RDC) is accessible.
     *
     * @param address The scheme://address form string locating the remote host on
     *                which the RDC is accessible.
     *
     * @throws BadHostException if the given host is invalid.
     */
    @Override
    public void setRDCAddress(String address) throws BadHostException {
        try {
            // Build an RDC location with a single address.
            Location location = new Location();
            location.addAddress(Address.make(address));
            service.setRDCLocation(location);
        } catch (MalformedAddressException ignored) {
            Log.e(getTag(), "Malformed address string given when setting RDC address.");
        }
    }

    /**
     * Set whether the middleware instance should be discoverable via the registered
     * RDC. When the middleware is non-discoverable, the RDC will not return the
     * middleware hostname as a response to queries from other middleware instances.
     *
     * @param discoverable Whether the middleware should be discoverable.
     */
    @Override
    public void setDiscoverable(boolean discoverable) {
        service.setDiscoverable(discoverable);
    }

    private static String getTag() {
        return "MW_BINDER";
    }
}
