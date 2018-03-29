package uk.ac.cam.seh208.middleware.binder;

import uk.ac.cam.seh208.middleware.common.MiddlewareCommand;
import uk.ac.cam.seh208.middleware.common.EndpointCommand;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.Query;


interface IMiddleware {
    // @see MiddlewareBinder#createEndpoint
    void createEndpoint(in EndpointDetails details, boolean exposed, boolean forceable);
    // @see MiddlewareBinder#destroyEndpoint
    void destroyEndpoint(String name);

    // @see MiddlewareBinder#getEndpointDetails
    EndpointDetails getEndpointDetails(String name);
    // @see MiddlewareBinder#getAllEndpointDetails
    List<EndpointDetails> getAllEndpointDetails();

    // @see MiddlewareBinder#force
    void force(String remote, in MiddlewareCommand command);
    // @see MiddlewareBinder#forceEndpoint
    void forceEndpoint(String remote, String name, in EndpointCommand command);
    // @see MiddlewareBinder#setForceable
    void setForceable(boolean forceable);

    // @see MiddlewareBinder#setRDCAddress
    void setRDCAddress(String address);
    // @see MiddlewareBinder#setDiscoverable
    void setDiscoverable(boolean discoverable);
}
