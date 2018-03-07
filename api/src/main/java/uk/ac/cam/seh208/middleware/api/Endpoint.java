package uk.ac.cam.seh208.middleware.api;

import android.os.DeadObjectException;
import android.os.RemoteException;
import android.util.Log;

import uk.ac.cam.seh208.middleware.api.exception.MiddlewareDisconnectedException;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.IMessageListener;
import uk.ac.cam.seh208.middleware.common.Persistence;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.common.exception.BadHostException;
import uk.ac.cam.seh208.middleware.common.exception.BadQueryException;
import uk.ac.cam.seh208.middleware.common.exception.EndpointNotFoundException;
import uk.ac.cam.seh208.middleware.common.exception.ListenerNotFoundException;
import uk.ac.cam.seh208.middleware.common.exception.MappingNotFoundException;
import uk.ac.cam.seh208.middleware.common.exception.ProtocolException;
import uk.ac.cam.seh208.middleware.common.exception.SchemaMismatchException;
import uk.ac.cam.seh208.middleware.common.exception.WrongPolarityException;


/**
 * Application-facing interface for an endpoint within a middleware instance.
 */
public class Endpoint {

    /**
     * Connection to the remote middleware service.
     */
    private MiddlewareServiceConnection connection;

    /**
     * Unique (within this middleware instance) name of the endpoint.
     */
    private String name;


    /**
     * Bind the given context to the endpoint via the middleware service, internally
     * tracking the service connection within this object.
     *
     * @param connection A connection to the remote service.
     * @param name Locally-unique name of the endpoint to bind to.
     */
    Endpoint(MiddlewareServiceConnection connection, String name) {
        this.connection = connection;
        this.name = name;
    }

    public EndpointDetails getDetails()
            throws MiddlewareDisconnectedException, EndpointNotFoundException {
        try {
            return connection.waitForBinder().mw_getEndpointDetails(name);
        } catch (EndpointNotFoundException e) {
            throw e;
        } catch (DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            Log.e(getTag(), "Unexpected exception thrown in getDetails.");
            return null;
        }
    }

    public void send(String message)
            throws MiddlewareDisconnectedException, WrongPolarityException,
                   SchemaMismatchException {
        try {
            connection.waitForBinder().ep_send(name, message);
        } catch (WrongPolarityException | SchemaMismatchException e) {
            throw e;
        } catch (DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            Log.e(getTag(), "Unexpected exception thrown in send.");
        }
    }

    public void registerListener(IMessageListener listener)
            throws MiddlewareDisconnectedException, RemoteException {
        try {
            connection.waitForBinder().ep_registerListener(name, listener);
        } catch (DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        }
    }

    public void unregisterListener(IMessageListener listener)
            throws MiddlewareDisconnectedException, ListenerNotFoundException {
        try {
            connection.waitForBinder().ep_unregisterListener(name, listener);
        } catch (ListenerNotFoundException e) {
            throw e;
        } catch (DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            Log.e(getTag(), "Unexpected exception thrown in unregisterListener.");
        }
    }

    public void clearListeners() throws MiddlewareDisconnectedException {
        try {
            connection.waitForBinder().ep_clearListeners(name);
        } catch (DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            Log.e(getTag(), "Unexpected exception thrown in clearListeners.");
        }
    }

    public long map(Query query, Persistence persistence)
            throws MiddlewareDisconnectedException, BadQueryException,
                   BadHostException, ProtocolException {
        try {
            return connection.waitForBinder().ep_map(name, query, persistence);
        } catch (BadQueryException | BadHostException | ProtocolException e) {
            throw e;
        } catch (DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            Log.e(getTag(), "Unexpected exception thrown in map.");
            return -1;
        }
    }

    public void unmap(long mappingId)
            throws MiddlewareDisconnectedException, MappingNotFoundException {
        try {
            connection.waitForBinder().ep_unmap(name, mappingId);
        } catch (MappingNotFoundException e) {
            throw e;
        } catch (DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            Log.e(getTag(), "Unexpected exception thrown in unmap.");
        }
    }

    public void unmapAll() throws MiddlewareDisconnectedException {
        try {
            connection.waitForBinder().ep_unmapAll(name);
        } catch (DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            Log.e(getTag(), "Unexpected exception thrown in unmapAll.");
        }
    }

    public int close(Query query) throws MiddlewareDisconnectedException {
        try {
            return connection.waitForBinder().ep_close(name, query);
        } catch (DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            Log.e(getTag(), "Unexpected exception thrown in close.");
            return -1;
        }
    }

    public int closeAll() throws MiddlewareDisconnectedException {
        try {
            return connection.waitForBinder().ep_closeAll(name);
        } catch (DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            Log.e(getTag(), "Unexpected exception thrown in closeAll.");
            return -1;
        }
    }

    public void setExposed(boolean exposed) throws MiddlewareDisconnectedException {
        try {
            connection.waitForBinder().ep_setExposed(name, exposed);
        } catch (DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            Log.e(getTag(), "Unexpected exception thrown in setExposed.");
        }
    }

    public void setForceable(boolean forceable) throws MiddlewareDisconnectedException {
        try {
            connection.waitForBinder().ep_setForceable(name, forceable);
        } catch (DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            Log.e(getTag(), "Unexpected exception thrown in setForceable.");
        }
    }

    private static String getTag() {
        return "MW";
    }
}
