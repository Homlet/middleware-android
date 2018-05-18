package uk.ac.cam.seh208.middleware.api;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import java.util.List;
import java.util.Objects;

import uk.ac.cam.seh208.middleware.common.EndpointCommand;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.IntentData;
import uk.ac.cam.seh208.middleware.common.MiddlewareCommand;
import uk.ac.cam.seh208.middleware.common.Polarity;
import uk.ac.cam.seh208.middleware.common.exception.BadHostException;

import static uk.ac.cam.seh208.middleware.api.RemoteUtils.callSafe;


/**
 * Application-facing interface for the middleware.
 */
@SuppressWarnings({"UnusedReturnValue", "SameParameterValue", "WeakerAccess", "unused"})
public class Middleware {

    /**
     * Reference to the owning context, on which binding takes place.
     */
    private Context context;

    /**
     * Connection to the remote middleware service.
     */
    private MiddlewareServiceConnection connection;


    /**
     * Store a reference to the owning context, on which binding is to take place.
     *
     * @param context Any context within the client application, on which the binding takes place.
     */
    public Middleware(Context context) {
        this.context = context;
        connection = new MiddlewareServiceConnection();
    }

    /**
     * Bind the owning context to the service, allowing communication to take
     * place. This method must be called before any API calls can take effect.
     */
    public void bind() {
        // Create a new intent for starting the middleware service (if not already started).
        Intent intent = new Intent();
        intent.setClassName(IntentData.MW_PACKAGE, IntentData.MW_NAME);
        context.startService(intent);

        // Reuse the intent for binding the owning context to the service.
        context.bindService(intent, connection, Context.BIND_IMPORTANT);
    }

    /**
     * Bind the owning context to the service, allowing a callback to be specified
     * to run once the binding has taken place.
     */
    public void bind(Runnable runnable) {
        connection.setCallback(runnable);
        bind();
    }

    /**
     * Unbind the owning context from the service to prevent leakage of
     * ServiceConnection resources. It is essential that this method is
     * called before the owning context is destroyed.
     */
    public void unbind() {
        context.unbindService(connection);
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

    public boolean doesEndpointExist(String name) throws MiddlewareDisconnectedException {
        List<EndpointDetails> details = getAllEndpointDetails();

        for (EndpointDetails endpoint : details) {
            if (Objects.equals(endpoint.getName(), name)) {
                return true;
            }
        }

        return false;
    }

    private void force(long uuid, MiddlewareCommand command)
            throws MiddlewareDisconnectedException {
        callSafe(() -> connection.waitForBinder().mw_force(uuid, command));
    }

    private void forceEndpoint(long uuid, String name, EndpointCommand command)
            throws MiddlewareDisconnectedException, BadHostException {
        callSafe(() -> connection.waitForBinder().mw_forceEndpoint(uuid, name, command));
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
