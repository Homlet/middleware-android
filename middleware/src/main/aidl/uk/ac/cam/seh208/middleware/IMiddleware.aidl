package uk.ac.cam.seh208.middleware;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;


interface IMiddleware {
    void createEndpoint(in EndpointDetails details);
    void destroyEndpoint(in String name);

    List<EndpointDetails> getAllEndpointDetails();
    EndpointDetails getEndpointDetails(in String name);

    void setRDC(in String host);
    void setDiscoverable(in boolean visible);
}
