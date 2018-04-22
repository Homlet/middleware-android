package uk.ac.cam.seh208.middleware.binder;

import android.os.RemoteException;

import uk.ac.cam.seh208.middleware.common.Persistence;
import uk.ac.cam.seh208.middleware.common.exception.BadHostException;
import uk.ac.cam.seh208.middleware.common.exception.BadQueryException;
import uk.ac.cam.seh208.middleware.common.IMessageListener;
import uk.ac.cam.seh208.middleware.common.exception.MappingNotFoundException;
import uk.ac.cam.seh208.middleware.common.exception.ProtocolException;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.common.exception.SchemaMismatchException;
import uk.ac.cam.seh208.middleware.common.exception.WrongPolarityException;
import uk.ac.cam.seh208.middleware.core.control.Endpoint;


/**
 * Implementation of the endpoint inter-process interface stub.
 *
 * IPC procedure calls are dispatched asynchronously from a thread pool
 * maintained by the Android runtime. Therefore, the objects acted upon
 * by this binder are correctly synchronised.
 *
 * Each active endpoint within the middleware may have an associated binder
 * object instantiated from this class. Therefore, the user may interact with
 * the middleware using object-oriented programming techniques.
 *
 * The interface is described in IEndpoint.aidl
 *
 * @see IEndpoint
 */
public class EndpointBinder extends IEndpoint.Stub {

    /**
     * Reference to the endpoint object exposed by this binder.
     */
    private Endpoint endpoint;


    EndpointBinder(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Drop the endpoint reference to allow garbage collection after the endpoint is destroyed.
     *
     * NOTE: after this call no further calls should be made.
     */
    void destroy() {
        endpoint = null;
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
     * @throws WrongPolarityException when the bound endpoint polarity does not permit sending.
     * @throws SchemaMismatchException when the message string does not match the endpoint schema.
     */
    @Override
    public void send(String message) throws WrongPolarityException, SchemaMismatchException {
        endpoint.send(message);
    }

    /**
     * Register a new message listener with the bound endpoint (provided the endpoint
     * polarity permits this). The listener must implement the IMessageListener interface
     * specified in AIDL.
     *
     * Whenever a new message is received on the bound endpoint, it is dispatched to all
     * of the registered listeners via a remote call to their onMessage methods.
     *
     * In the case that the process hosting the listener implementation terminates, the
     * listener will automatically be removed from the endpoint. Therefore, listening
     * processes should re-register listeners when continuing after being killed.
     *
     * @param listener Object implementing the IMessageListener interface, which will be
     *                 remoted by Android allowing the middleware to call its methods.
     *
     * @throws RemoteException when the linkToDeath procedure fails for the listener.
     */
    @Override
    public void registerListener(IMessageListener listener) throws RemoteException {
        endpoint.registerListener(listener);
    }

    /**
     * Unregister a message listener from the bound endpoint. Once unregistered, the
     * listener will no longer be invoked when new messages arrive on the endpoint.
     *
     * @param listener Object previously remoted and registered as a listener.
     */
    @Override
    public void unregisterListener(IMessageListener listener) throws RemoteException {
        endpoint.unregisterListener(listener);
    }

    /**
     * Unregister all message listeners from the bound endpoint.
     */
    @Override
    public void clearListeners() {
        endpoint.clearListeners();
    }

    /**
     * Perform an RDC-indirect mapping on the bound endpoint. This consists of two stages:
     *
     *   1. use the registered RDC to discover the locations of peers exposing endpoints
     *      matching the given query;
     *   2. establish a mapping by opening channels to one or more endpoints from one
     *      or more of these peers.
     *
     * The schema and polarity fields in the given query must be left empty; these are
     * determined by the schema and polarity of the bound endpoint.
     *
     * If the given query allows only a limited number of matches, a new query object
     * is constructed for each peer, allowing only the number of matches remaining
     * after channels were established with previously contacted peers. Peers are
     * contacted in the order returned by the RDC.
     *
     * In the case that any resources returned from the RDC time out or break protocol
     * whilst being contacted, these are collated and sent to the RDC at the end of the
     * operation to aid RDC bookkeeping.
     *
     * The return value is the unique identifier of the newly created mapping.
     *
     * @param query An endpoint query object for resource discovery and filtering
     *              remote endpoints.
     *
     * @return a unique long identifier of the newly created mapping.
     *
     * @throws BadQueryException if either of the schema or polarity fields are set in the query.
     * @throws BadHostException when the set RDC host is invalid.
     * @throws ProtocolException if the RDC breaks protocol.
     */
    @Override
    public long map(Query query, Persistence persistence)
            throws BadQueryException, BadHostException, ProtocolException {
        return endpoint.map(query, persistence).getMappingId();
    }

    /**
     * Close a mapping associated with this endpoint, referenced by its unique mapping
     * identifier. The process of closing the mapping will close any remaining owned
     * channels.
     *
     * @param mappingId Unique long identifier of the mapping.
     *
     * @throws MappingNotFoundException if the mapping identifier is not recognised
     *                                  for this endpoint.
     */
    @Override
    public void unmap(long mappingId) throws MappingNotFoundException {
        endpoint.unmap(mappingId);
    }

    /**
     * Close all active mappings on this endpoint. The process of closing each mapping
     * will close any remaining owned channels.
     *
     * Note that channels initiated by remote mappings will remain open after this call.
     * To close all remaining channels following this call, use EndpointBinder#closeAll
     */
    @Override
    public void unmapAll() {
        endpoint.unmapAll();
    }

    /**
     * Close all channels from this endpoint which match a given query.
     *
     * @param query Query used to filter the channels from this endpoint.
     *
     * @return the number of channels that were closed.
     */
    @Override
    public int close(Query query) {
        return endpoint.closeChannels(query);
    }

    /**
     * Close all channels from this endpoint.
     *
     * @return the number of channels that were closed.
     */
    @Override
    public int closeAll() {
        return endpoint.closeAllChannels();
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
        endpoint.setExposed(exposed);
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
        endpoint.setForceable(forceable);
    }
}
