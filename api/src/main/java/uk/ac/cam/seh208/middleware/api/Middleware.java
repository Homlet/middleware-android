package uk.ac.cam.seh208.middleware.api;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import java.util.List;

import uk.ac.cam.seh208.middleware.api.exception.MiddlewareDisconnectedException;
import uk.ac.cam.seh208.middleware.common.EndpointCommand;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.IntentData;
import uk.ac.cam.seh208.middleware.common.MiddlewareCommand;
import uk.ac.cam.seh208.middleware.common.Polarity;
import uk.ac.cam.seh208.middleware.common.exception.BadHostException;
import uk.ac.cam.seh208.middleware.common.exception.BadSchemaException;
import uk.ac.cam.seh208.middleware.common.exception.EndpointCollisionException;

import static uk.ac.cam.seh208.middleware.api.RemoteUtils.callSafe;


/**
 * Application-facing interface for the middleware.
 */
public class Middleware {

    /**
     * Connection to the remote middleware service.
     */
    private MiddlewareServiceConnection connection;


    /**
     * Bind the given context to the middleware service, internally tracking the
     * service connection within this object.
     *
     * @param context Any context within the client application, on which the binding takes place.
     */
    public Middleware(Context context) throws MiddlewareDisconnectedException {
        // Create a new intent for starting the middleware service (if not already started).
        Intent intent = new Intent();
        intent.setClassName(IntentData.SERVICE_PACKAGE, IntentData.SERVICE_NAME);
        context.startService(intent);

        // Create a binding intent for the service and bind the given context to it.
        connection = new MiddlewareServiceConnection();
        context.bindService(intent, connection, Context.BIND_IMPORTANT);
    }


    public Endpoint getEndpoint(@NonNull String name) {
        return new Endpoint(connection, name);
    }

    public Endpoint createSource(@NonNull String name, String desc,
                                 String schema, List<String> tags,
                                 boolean exposed, boolean forceable)
            throws MiddlewareDisconnectedException {
        createEndpoint(
                new EndpointDetails(name, desc, Polarity.SOURCE, schema, tags),
                exposed,
                forceable);
        return getEndpoint(name);
    }

    public Endpoint createSink(@NonNull String name, String desc,
                               String schema, List<String> tags,
                               boolean exposed, boolean forceable)
            throws MiddlewareDisconnectedException {
        createEndpoint(
                new EndpointDetails(name, desc, Polarity.SINK, schema, tags),
                exposed,
                forceable);
        return getEndpoint(name);
    }

    private void createEndpoint(EndpointDetails details, boolean exposed, boolean forceable)
            throws MiddlewareDisconnectedException {
        callSafe(() -> connection.waitForBinder().mw_createEndpoint(details, exposed, forceable));
    }

    public void destroyEndpoint(String name) throws MiddlewareDisconnectedException {
        callSafe(() -> connection.waitForBinder().mw_destroyEndpoint(name));
    }

    public EndpointDetails getEndpointDetails(String name) throws MiddlewareDisconnectedException {
        return callSafe(() -> connection.waitForBinder().mw_getEndpointDetails(name));
    }

    public List<EndpointDetails> getAllEndpointDetails() throws MiddlewareDisconnectedException {
        return callSafe(() -> connection.waitForBinder().mw_getAllEndpointDetails());
    }

    private void force(String remote, MiddlewareCommand command)
            throws MiddlewareDisconnectedException {
        callSafe(() -> connection.waitForBinder().mw_force(remote, command));
    }

    private void forceEndpoint(String remote, String name, EndpointCommand command)
            throws MiddlewareDisconnectedException, BadHostException {
        callSafe(() -> connection.waitForBinder().mw_forceEndpoint(remote, name, command));
    }

    public void setForceable(boolean forceable) throws MiddlewareDisconnectedException {
        callSafe(() -> connection.waitForBinder().mw_setForceable(forceable));
    }

    public void setRDCAddress(String address) throws MiddlewareDisconnectedException {
        callSafe(() -> connection.waitForBinder().mw_setRDCAddress(address));
    }

    public void setDiscoverable(boolean discoverable) throws MiddlewareDisconnectedException {
        callSafe(() -> connection.waitForBinder().mw_setDiscoverable(discoverable));
    }

    private static String getTag() {
        return "MW";
    }
}
