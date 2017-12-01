package uk.ac.cam.seh208.middleware.binder;

import android.os.RemoteException;

import java.util.List;

import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.common.RemoteEndpointDetails;
import uk.ac.cam.seh208.middleware.core.MiddlewareService;


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
     * Reference to the running instance of the endpoint service.
     */
    private final MiddlewareService service;

    /**
     * Unique name of the endpoint this binder interacts with.
     */
    private final String name;


    public EndpointBinder(MiddlewareService service, String name) {
        this.service = service;
        this.name = name;
    }

    @Override
    public void send(String message) throws RemoteException {
        // TODO: implement.
    }

    @Override
    public List<RemoteEndpointDetails> getPeers() throws RemoteException {
        // TODO: implement.
        return null;
    }

    @Override
    public List<RemoteEndpointDetails> map(Query query) throws RemoteException {
        // TODO: implement.
        return null;
    }

    @Override
    public List<RemoteEndpointDetails> mapTo(String host, Query query) throws RemoteException {
        // TODO: implement.
        return null;
    }

    @Override
    public List<RemoteEndpointDetails> unmap(Query query) throws RemoteException {
        // TODO: implement.
        return null;
    }

    @Override
    public List<RemoteEndpointDetails> unmapFrom(String host, Query query) throws RemoteException {
        // TODO: implement.
        return null;
    }

    @Override
    public List<RemoteEndpointDetails> unmapAll() throws RemoteException {
        // TODO: implement.
        return null;
    }

    @Override
    public void setExposed(boolean exposed) throws RemoteException {
        // TODO: implement.
    }
}
