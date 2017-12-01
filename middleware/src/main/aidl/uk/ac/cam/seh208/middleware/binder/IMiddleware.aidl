package uk.ac.cam.seh208.middleware.binder;

import uk.ac.cam.seh208.middleware.common.Command;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.Query;


interface IMiddleware {
    // @see MiddlewareBinder#createEndpoint
    void createEndpoint(in EndpointDetails details);
    // @see MiddlewareBinder#destroyEndpoint
    void destroyEndpoint(in String name);

    // @see MiddlewareBinder#getEndpointDetails
    EndpointDetails getEndpointDetails(in String name);
    // @see MiddlewareBinder#getAllEndpointDetails
    List<EndpointDetails> getAllEndpointDetails();

    // @see MiddlewareBinder#force
    void force(in String host, in Command command);

    // @see MiddlewareBinder#setRDCHost
    void setRDCHost(in String host);
    // @see MiddlewareBinder#setDiscoverable
    void setDiscoverable(in boolean discoverable);
    // @see MiddlewareBinder#discover
    List<String> discover(in Query query);
}
