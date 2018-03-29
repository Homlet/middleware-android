package uk.ac.cam.seh208.middleware.binder;

import uk.ac.cam.seh208.middleware.common.EndpointCommand;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.IMessageListener;
import uk.ac.cam.seh208.middleware.common.MiddlewareCommand;
import uk.ac.cam.seh208.middleware.common.Persistence;
import uk.ac.cam.seh208.middleware.common.Query;


// TODO: script generation of this file from the other two interfaces.
interface ICombined {
    /* ======== Beginning of middleware interface ========================== */

    void mw_createEndpoint(in EndpointDetails details, boolean exposed, boolean forceable);
    void mw_destroyEndpoint(String name);

    EndpointDetails mw_getEndpointDetails(String name);
    List<EndpointDetails> mw_getAllEndpointDetails();

    void mw_force(String remote, in MiddlewareCommand command);
    void mw_forceEndpoint(String remote, String name, in EndpointCommand command);
    void mw_setForceable(boolean forceable);

    void mw_setRDCAddress(String address);
    void mw_setDiscoverable(boolean discoverable);


    /* ======== Beginning of endpoint interface ============================ */

    void ep_send(String name, String message);

    void ep_registerListener(String name, in IMessageListener listener);
    void ep_unregisterListener(String name, in IMessageListener listener);
    void ep_clearListeners(String name);

    long ep_map(String name, in Query query, in Persistence persistence);
    void ep_unmap(String name, long mappingId);
    void ep_unmapAll(String name);
    int ep_close(String name, in Query query);
    int ep_closeAll(String name);

    void ep_setExposed(String name, boolean exposed);
    void ep_setForceable(String name, boolean forceable);
}
