package uk.ac.cam.seh208.middleware.binder;

import android.os.RemoteException;
import android.util.ArrayMap;

import java.util.List;
import java.util.Map;

import uk.ac.cam.seh208.middleware.common.EndpointCommand;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.IMessageListener;
import uk.ac.cam.seh208.middleware.common.MiddlewareCommand;
import uk.ac.cam.seh208.middleware.common.Persistence;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.core.MiddlewareService;
import uk.ac.cam.seh208.middleware.core.comms.Endpoint;


/**
 * Combined binder used as a workaround for Android incorrectly
 * caching binders to the same client context.
 */
public class CombinedBinder extends ICombined.Stub {

    /**
     * Reference to the owning middleware service.
     */
    private MiddlewareService service;

    /**
     * Binder object for handling general middleware IPC calls.
     */
    private MiddlewareBinder middlewareBinder;

    /**
     * Collection of binder objects for handling endpoint-specific
     * IPC calls on a particular endpoint. Indexed by endpoint name.
     */
    private Map<String, EndpointBinder> endpointBinders;


    public CombinedBinder(MiddlewareService service) {
        this.service = service;

        middlewareBinder = new MiddlewareBinder(service);
        endpointBinders = new ArrayMap<>();
    }

    private synchronized EndpointBinder getEndpointBinder(String name) {
        if (endpointBinders.containsKey(name)) {
            return endpointBinders.get(name);
        }

        Endpoint endpoint = service.getEndpointSet().getEndpointByName(name);
        EndpointBinder binder = new EndpointBinder(endpoint);
        endpointBinders.put(name, binder);
        return binder;
    }

    /**
     * When an endpoint is destroyed, this method should be called to prevent
     * old binders remaining valid.
     */
    public synchronized void invalidateEndpoint(String name) {
        EndpointBinder binder = endpointBinders.remove(name);

        if (binder != null) {
            binder.destroy();
        }
    }

    @Override
    public void mw_createEndpoint(EndpointDetails details, boolean exposed, boolean forceable)
            throws RemoteException {
        middlewareBinder.createEndpoint(details, exposed, forceable);
    }

    @Override
    public synchronized void mw_destroyEndpoint(String name) throws RemoteException {
        middlewareBinder.destroyEndpoint(name);
    }

    @Override
    public EndpointDetails mw_getEndpointDetails(String name) throws RemoteException {
        return middlewareBinder.getEndpointDetails(name);
    }

    @Override
    public List<EndpointDetails> mw_getAllEndpointDetails() throws RemoteException {
        return middlewareBinder.getAllEndpointDetails();
    }

    @Override
    public void mw_force(String remote, MiddlewareCommand command) throws RemoteException {
        middlewareBinder.force(remote, command);
    }

    @Override
    public void mw_forceEndpoint(String remote, String name, EndpointCommand command)
            throws RemoteException {
        middlewareBinder.forceEndpoint(remote, name, command);
    }

    @Override
    public void mw_setForceable(boolean forceable) throws RemoteException {
        middlewareBinder.setForceable(forceable);
    }

    @Override
    public void mw_setRDCAddress(String address) throws RemoteException {
        middlewareBinder.setRDCAddress(address);
    }

    @Override
    public void mw_setDiscoverable(boolean discoverable) throws RemoteException {
        middlewareBinder.setDiscoverable(discoverable);
    }

    @Override
    public List<String> mw_discover(Query query) throws RemoteException {
        return middlewareBinder.discover(query);
    }

    @Override
    public void ep_send(String name, String message) throws RemoteException {
        getEndpointBinder(name).send(message);
    }

    @Override
    public void ep_registerListener(String name, IMessageListener listener)
            throws RemoteException {
        getEndpointBinder(name).registerListener(listener);
    }

    @Override
    public void ep_unregisterListener(String name, IMessageListener listener)
            throws RemoteException {
        getEndpointBinder(name).unregisterListener(listener);
    }

    @Override
    public void ep_clearListeners(String name) throws RemoteException {
        getEndpointBinder(name).clearListeners();
    }

    @Override
    public long ep_map(String name, Query query, Persistence persistence)
            throws RemoteException {
        return getEndpointBinder(name).map(query, persistence);
    }

    @Override
    public void ep_unmap(String name, long mappingId) throws RemoteException {
        getEndpointBinder(name).unmap(mappingId);
    }

    @Override
    public void ep_unmapAll(String name) throws RemoteException {
        getEndpointBinder(name).unmapAll();
    }

    @Override
    public int ep_close(String name, Query query) throws RemoteException {
        return getEndpointBinder(name).close(query);
    }

    @Override
    public int ep_closeAll(String name) throws RemoteException {
        return getEndpointBinder(name).closeAll();
    }

    @Override
    public void ep_setExposed(String name, boolean exposed) throws RemoteException {
        getEndpointBinder(name).setExposed(exposed);
    }

    @Override
    public void ep_setForceable(String name, boolean forceable) throws RemoteException {
        getEndpointBinder(name).setForceable(forceable);
    }
}
