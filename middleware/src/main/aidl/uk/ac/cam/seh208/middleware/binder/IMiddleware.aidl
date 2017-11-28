package uk.ac.cam.seh208.middleware.binder;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.Query;


interface IMiddleware {
    void createEndpoint(in EndpointDetails details);
    void destroyEndpoint(in String name);

    List<EndpointDetails> getAllEndpointDetails();
    EndpointDetails getEndpointDetails(in String name);

    void force(in String host, in Command command);

    void setRDC(in String host);
    void setDiscoverable(in boolean discoverable);
    List<String> discover(in Query query);
}
