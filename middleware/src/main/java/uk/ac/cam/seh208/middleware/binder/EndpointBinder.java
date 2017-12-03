package uk.ac.cam.seh208.middleware.binder;

import android.os.RemoteException;

import java.util.List;

import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.common.RemoteEndpointDetails;
import uk.ac.cam.seh208.middleware.core.MiddlewareService;


// TODO: differentiate RemoteException types by class.
/**
 * Implementation of the endpoint inter-process interface stub.
 *
 * This is defined in a thread-safe manner, as IPC procedure calls
 * are dispatched asynchronously from a thread pool maintained by
 * the Android runtime.
 *
 * Each endpoint within the middleware has an associated binder object
 * instantiated from this class. Therefore, the user may interact with
 * the middleware using object-oriented programming techniques.
 *
 * The interface is described in IEndpoint.aidl
 *
 * @see IEndpoint
 */
public class EndpointBinder extends IEndpoint.Stub {
    /**
     * Reference to the running instance of the middleware service.
     */
    private final MiddlewareService service;

    /**
     * Unique name of the endpoint bound to this interface.
     */
    private final String name;


    public EndpointBinder(MiddlewareService service, String name) {
        this.service = service;
        this.name = name;
    }

    /**
     * Send a JSON message over the bound endpoint (provided the endpoint polarity permits
     * this). The message must conform to the endpoint message schema; if not, an exception
     * will be thrown.
     *
     * Sent messages are broadcast to all mapped peer endpoints on remote instances of the
     * middleware. Applications on the remote instance may receive incoming messages by
     * registering a listener with the peer endpoint.
     *
     * @param message JSON string representation of the message to send.
     *
     * @throws RemoteException when the bound endpoint polarity does not permit sending.
     * @throws RemoteException when the message string does not match the endpoint schema.
     */
    @Override
    public void send(String message) throws RemoteException {
        // TODO: implement.
    }

    /**
     * Get a list of all mapped peer endpoint details.
     *
     * @return a list of RemoteEndpointDetails objects.
     */
    @Override
    public List<RemoteEndpointDetails> getPeers() {
        // TODO: implement.
        return null;
    }

    /**
     * Perform an indirect mapping on the bound endpoint. This consists of two stages:
     *
     *   1. use the registered RDC to discover the locations of peers exposing endpoints
     *      matching the given query;
     *   2. establish mappings with one or more endpoints from one or more of these
     *      peers, by sending queries to each in turn.
     *
     * The schema and polarity fields in the given query must be left empty; these are
     * determined by the schema and polarity of the bound endpoint.
     *
     * If the given query allows only a limited number of matches, a new query object
     * is constructed for each peer, allowing only the number of matches remaining
     * after mappings were established with previously contacted peers. Peers are
     * contacted in the order returned by the RDC.
     *
     * In the case that any resources returned from the RDC time out or break protocol
     * whilst being contacted, these are collated and sent to the RDC at the end of the
     * operation to aid RDC bookkeeping.
     *
     * The return value is a list of the endpoints that were successfully mapped to.
     *
     * @param query An endpoint query object for resource discovery and filtering
     *              remote endpoints.
     *
     * @return a list of RemoteEndpointDetails objects representing the endpoints that were
     *         successfully mapped to during the operation.
     *
     * @throws RemoteException if either of the schema or polarity fields are set in the query.
     * @throws RemoteException if the RDC location is not set in the middleware.
     * @throws RemoteException if the RDC times out or breaks protocol.
     */
    @Override
    public List<RemoteEndpointDetails> map(Query query) throws RemoteException {
        // TODO: implement.
        return null;
    }

    /**
     * Perform a direct mapping on the bound endpoint. This is equivalent to the second stage
     * of an indirect mapping: the query is sent to a given peer in order to establish
     * mappings with remote endpoints.
     *
     * The schema and polarity fields in the given query must be left empty; these are
     * determined by the schema and polarity of the bound endpoint.
     *
     * In the case that the given host times out or breaks protocol whilst being contacted,
     * an exception will be thrown.
     *
     * The return value is a list of the endpoints that were successfully mapped to.
     *
     * @param host Hostname or address of the peer.
     * @param query An endpoint query object for filtering remote endpoints.
     *
     * @return a list of RemoteEndpointDetails objects representing the endpoints that were
     *         successfully mapped to during the operation.
     *
     * @throws RemoteException if the hostname is an invalid URL.
     * @throws RemoteException if the given host times out or breaks protocol.
     * @throws RemoteException if either of the schema or polarity fields are set in the query.
     */
    @Override
    public List<RemoteEndpointDetails> mapTo(String host, Query query) throws RemoteException {
        // TODO: implement.
        return null;
    }

    /**
     * Gracefully tear down any remote mappings to the bound endpoint whose remote endpoints
     * match the given query. Note that the query does not take into account the location of
     * the peer on which the remote endpoint resides; to discriminate by host, use unmapFrom.
     *
     * The schema and polarity fields in the given query must be left empty; these are
     * determined by the schema and polarity of the bound endpoint.
     *
     * The return value is a list of the endpoints that were unmapped from.
     *
     * @param query An endpoint query object for filtering remote endpoints.
     *
     * @return a list of RemoteEndpointDetails object representing the endpoints that were
     *         unmapped from during the operation.
     *
     * @throws RemoteException if either of the schema or polarity fields are set in the query.
     */
    @Override
    public List<RemoteEndpointDetails> unmap(Query query) throws RemoteException {
        // TODO: implement.
        return null;
    }


    /**
     * Gracefully tear down any remote mappings to the bound endpoint whose remote endpoints
     * match the given query, and whose peer resides on the given host.
     *
     * The schema and polarity fields in the given query must be left empty; these are
     * determined by the schema and polarity of the bound endpoint.
     *
     * The return value is a list of the endpoints that were unmapped from.
     *
     * @param host Hostname or address of the peer.
     * @param query An endpoint query object for filtering remote endpoints.
     *
     * @return a list of RemoteEndpointDetails object representing the endpoints that were
     *         unmapped from during the operation.
     *
     * @throws RemoteException if the hostname is an invalid URL.
     * @throws RemoteException if either of the schema or polarity fields are set in the query.
     */
    @Override
    public List<RemoteEndpointDetails> unmapFrom(String host, Query query) throws RemoteException {
        // TODO: implement.
        return null;
    }

    /**
     * Set whether the bound endpoint should be exposed to remote instances of the middleware;
     * i.e. whether the middleware should allow remote middleware instances to establish
     * mappings with the bound endpoint if it matches a received query.
     *
     * @param exposed Whether the endpoint should be exposed to remote instances
     *                of the middleware.
     */
    @Override
    public void setExposed(boolean exposed) {
        service.getEndpointSet()
                .getEndpointByName(name)
                .setExposed(exposed);
    }

    /**
     * Specify whether it should be possible for remote instances of the middleware
     * to force commands to run on the bound endpoint.
     *
     * NOTE: when the forceable field is set as false for the middleware,
     *       endpoint-specific values are ignored.
     *
     * @param forceable Whether the bound endpoint accepts remote commands.
     */
    @Override
    public void setForceable(boolean forceable) {
        service.getEndpointSet()
                .getEndpointByName(name)
                .setForceable(forceable);
    }
}
