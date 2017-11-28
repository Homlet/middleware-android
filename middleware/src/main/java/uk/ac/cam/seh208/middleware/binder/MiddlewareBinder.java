package uk.ac.cam.seh208.middleware.binder;

import android.os.RemoteException;

import java.util.List;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.core.EndpointCollisionException;
import uk.ac.cam.seh208.middleware.core.EndpointNotFoundException;
import uk.ac.cam.seh208.middleware.core.MiddlewareService;


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

    @Override
    public void createEndpoint(EndpointDetails details) throws RemoteException {
        try {
            service.createEndpoint(details);
        } catch (EndpointCollisionException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void destroyEndpoint(String name) throws RemoteException {
        try {
            service.destroyEndpoint(name);
        } catch(EndpointNotFoundException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public List<EndpointDetails> getAllEndpointDetails() throws RemoteException {
        // TODO: implement.
        return null;
    }

    @Override
    public EndpointDetails getEndpointDetails(String name) throws RemoteException {
        // TODO: implement.
        return null;
    }

    @Override
    public void setRDC(String host) throws RemoteException {
        // TODO: implement.
    }

    @Override
    public void setDiscoverable(boolean visible) throws RemoteException {
        // TODO: implement.
    }
}
