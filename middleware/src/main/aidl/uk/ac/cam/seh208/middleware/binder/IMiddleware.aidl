package uk.ac.cam.seh208.middleware.binder;

import uk.ac.cam.seh208.middleware.common.MiddlewareCommand;
import uk.ac.cam.seh208.middleware.common.EndpointCommand;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.Query;


interface IMiddleware {
    // @see MiddlewareBinder#createEndpoint
    void createEndpoint(in EndpointDetails details, in boolean exposed, in boolean forceable);
    // @see MiddlewareBinder#destroyEndpoint
    void destroyEndpoint(in String name);

    // @see MiddlewareBinder#getEndpointDetails
    EndpointDetails getEndpointDetails(in String name);
    // @see MiddlewareBinder#getAllEndpointDetails
    List<EndpointDetails> getAllEndpointDetails();

    // @see MiddlewareBinder#force
    void force(in String host, in MiddlewareCommand command);
    // @see MiddlewareBinder#forceEndpoint
    void forceEndpoint(in String host, in String name, in EndpointCommand command);
    // @see MiddlewareBinder#setForceable
    void setForceable(in boolean forceable);

    // @see MiddlewareBinder#setRDCHost
    void setRDCHost(in String host);
    // @see MiddlewareBinder#setDiscoverable
    void setDiscoverable(in boolean discoverable);
    // @see MiddlewareBinder#discover
    List<String> discover(in Query query);
}
