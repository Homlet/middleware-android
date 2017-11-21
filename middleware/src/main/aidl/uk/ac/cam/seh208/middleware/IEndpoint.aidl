package uk.ac.cam.seh208.middleware;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.RemoteEndpointDetails;
import uk.ac.cam.seh208.middleware.common.Query;


interface IEndpoint {
    void send(in String message);

//    void registerListener( ... );
//    void unregisterListener( ... );
//    void clearListeners( ... );

    List<RemoteEndpointDetails> getPeers();
    List<RemoteEndpointDetails> map(in String host, in Query query);
    List<RemoteEndpointDetails> unmap(in Query query);
    List<RemoteEndpointDetails> unmapFrom(in String host);
    List<RemoteEndpointDetails> unmapAll();
}
