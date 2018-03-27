package uk.ac.cam.seh208.middleware.api;

import uk.ac.cam.seh208.middleware.api.exception.MiddlewareDisconnectedException;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.IMessageListener;
import uk.ac.cam.seh208.middleware.common.Persistence;
import uk.ac.cam.seh208.middleware.common.Query;

import static uk.ac.cam.seh208.middleware.api.RemoteUtils.callSafe;


/**
 * Application-facing interface for an endpoint within a middleware instance.
 */
@SuppressWarnings({"UnusedReturnValue", "SameParameterValue", "WeakerAccess", "unused"})
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

    public EndpointDetails getDetails() throws MiddlewareDisconnectedException {
        return callSafe(() -> connection.waitForBinder().mw_getEndpointDetails(name));
    }

    public void send(String message) throws MiddlewareDisconnectedException {
        callSafe(() -> connection.waitForBinder().ep_send(name, message));
    }

    public void registerListener(IMessageListener listener)
            throws MiddlewareDisconnectedException {
        callSafe(() -> connection.waitForBinder().ep_registerListener(name, listener));
    }

    public void unregisterListener(IMessageListener listener)
            throws MiddlewareDisconnectedException {
        callSafe(() -> connection.waitForBinder().ep_unregisterListener(name, listener));
    }

    public void clearListeners() throws MiddlewareDisconnectedException {
        callSafe(() -> connection.waitForBinder().ep_clearListeners(name));
    }

    public long map(Query query, Persistence persistence) throws MiddlewareDisconnectedException {
        // NOTE: Unboxing of null cannot occur as null is returned only in an unreachable case.
        //noinspection ConstantConditions
        return callSafe(() -> connection.waitForBinder().ep_map(name, query, persistence));
    }

    public void unmap(long mappingId) throws MiddlewareDisconnectedException {
        callSafe(() -> connection.waitForBinder().ep_unmap(name, mappingId));
    }

    public void unmapAll() throws MiddlewareDisconnectedException {
        callSafe(() -> connection.waitForBinder().ep_unmapAll(name));
    }

    public int close(Query query) throws MiddlewareDisconnectedException {
        // NOTE: Unboxing of null cannot occur as null is returned only in an unreachable case.
        //noinspection ConstantConditions
        return callSafe(() -> connection.waitForBinder().ep_close(name, query));
    }

    public int closeAll() throws MiddlewareDisconnectedException {
        // NOTE: Unboxing of null cannot occur as null is returned only in an unreachable case.
        //noinspection ConstantConditions
        return callSafe(() -> connection.waitForBinder().ep_closeAll(name));
    }

    public void setExposed(boolean exposed) throws MiddlewareDisconnectedException {
        callSafe(() -> connection.waitForBinder().ep_setExposed(name, exposed));
    }

    public void setForceable(boolean forceable) throws MiddlewareDisconnectedException {
        callSafe(() -> connection.waitForBinder().ep_setForceable(name, forceable));
    }

    private static String getTag() {
        return "MW";
    }
}
