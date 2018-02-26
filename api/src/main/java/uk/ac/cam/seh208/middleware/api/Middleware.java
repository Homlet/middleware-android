package uk.ac.cam.seh208.middleware.api;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;

import uk.ac.cam.seh208.api.R;
import uk.ac.cam.seh208.middleware.api.exception.MiddlewareDisconnectedException;
import uk.ac.cam.seh208.middleware.binder.IMiddleware;
import uk.ac.cam.seh208.middleware.common.EndpointCommand;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.MiddlewareCommand;
import uk.ac.cam.seh208.middleware.common.exception.BadHostException;
import uk.ac.cam.seh208.middleware.common.exception.BadSchemaException;
import uk.ac.cam.seh208.middleware.common.exception.EndpointCollisionException;
import uk.ac.cam.seh208.middleware.common.exception.EndpointNotFoundException;

import static android.content.Context.BIND_IMPORTANT;


/**
 * Application-facing interface for the middleware
 */
public class Middleware {

    /**
     * Binder used to communicate with the
     */
    private IMiddleware binder;


    /**
     * Bind the given context to the middleware service, internally tracking the
     * service connection within this object.
     *
     * @param context Any context within the client application, on which the binding takes place.
     */
    public Middleware(Context context) {
        // Create a new intent for starting the middleware service (if not already started).
        Intent serviceIntent =
                new Intent(context.getString(R.string.middleware_service_start_intent));
        context.startService(serviceIntent);

        // Create a binding intent for the service and bind the given context to it.
        Intent bindIntent = new Intent(context.getString(R.string.middleware_service_bind_intent));
        bindIntent.setPackage(context.getString(R.string.middleware_service_package));
        ServiceConnection connection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                binder = IMiddleware.Stub.asInterface(service);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.e(getTag(), "Middleware service unexpectedly disconnected from client.");
                binder = null;
            }
        };
        context.bindService(bindIntent, connection, BIND_IMPORTANT);
    }

    private void createEndpoint(EndpointDetails details, boolean exposed, boolean forceable)
            throws MiddlewareDisconnectedException, EndpointCollisionException,
                   BadSchemaException {
        try {
            binder.createEndpoint(details, exposed, forceable);
        } catch (EndpointCollisionException | BadSchemaException e) {
            throw e;
        } catch (NullPointerException | DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            Log.e(getTag(), "Unexpected exception thrown in createEndpoint.");
        }
    }

    public void destroyEndpoint(String name)
            throws MiddlewareDisconnectedException, EndpointNotFoundException {
        try {
            binder.destroyEndpoint(name);
        } catch (EndpointNotFoundException e) {
            throw e;
        } catch (NullPointerException | DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            Log.e(getTag(), "Unexpected exception thrown in destroyEndpoint.");
        }
    }

    public EndpointDetails getEndpointDetails(String name)
            throws MiddlewareDisconnectedException, EndpointNotFoundException {
        try {
            return binder.getEndpointDetails(name);
        } catch (EndpointNotFoundException e) {
            throw e;
        } catch (NullPointerException | DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            Log.e(getTag(), "Unexpected exception thrown in getEndpointDetails.");
            return null;
        }
    }

    public List<EndpointDetails> getAllEndpointDetails()
            throws MiddlewareDisconnectedException {
        try {
            return binder.getAllEndpointDetails();
        } catch (NullPointerException | DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            Log.e(getTag(), "Unexpected exception thrown in getAllEndpointDetails.");
            return null;
        }
    }

    private void force(String remote, MiddlewareCommand command)
            throws MiddlewareDisconnectedException, BadHostException {
        try {
            binder.force(remote, command);
        } catch (BadHostException e) {
            throw e;
        } catch (NullPointerException | DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            Log.e(getTag(), "Unexpected exception thrown in force.");
        }
    }

    private void forceEndpoint(String remote, String name, EndpointCommand command)
            throws MiddlewareDisconnectedException, BadHostException {
        try {
            binder.forceEndpoint(remote, name, command);
        } catch (BadHostException e) {
            throw e;
        } catch (NullPointerException | DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            Log.e(getTag(), "Unexpected exception thrown in forceEndpoint.");
        }
    }

    public void setForceable(boolean forceable)
            throws MiddlewareDisconnectedException {
        try {
            binder.setForceable(forceable);
        } catch (NullPointerException | DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            Log.e(getTag(), "Unexpected exception thrown in setForceable.");
        }
    }

    public void setRDCAddress(String address)
            throws MiddlewareDisconnectedException, BadHostException {
        try {
            binder.setRDCAddress(address);
        } catch (BadHostException e) {
            throw e;
        } catch (NullPointerException | DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            Log.e(getTag(), "Unexpected exception thrown in setRDCAddress.");
        }
    }

    public void setDiscoverable(boolean discoverable)
            throws MiddlewareDisconnectedException {
        try {
            binder.setDiscoverable(discoverable);
        } catch (NullPointerException | DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            Log.e(getTag(), "Unexpected exception thrown in setDiscoverable.");
        }
    }

    private static String getTag() {
        return "MW";
    }
}
