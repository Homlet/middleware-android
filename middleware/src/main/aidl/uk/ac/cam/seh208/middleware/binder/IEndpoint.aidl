package uk.ac.cam.seh208.middleware.binder;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.RemoteEndpointDetails;
import uk.ac.cam.seh208.middleware.common.Query;


interface IEndpoint {
    // @see EndpointBinder#send
    void send(in String message);

//    // @see EndpointBinder#registerListener
//    void registerListener( ... );
//    // @see EndpointBinder#unregisterListener
//    void unregisterListener( ... );
//    // @see EndpointBinder#clearListeners
//    void clearListeners( ... );

    // @see EndpointBinder#getPeers
    List<RemoteEndpointDetails> getPeers();
    // @see EndpointBinder#map
    List<RemoteEndpointDetails> map(in Query query);
    // @see EndpointBinder#mapTo
    List<RemoteEndpointDetails> mapTo(in String host, in Query query);
    // @see EndpointBinder#unmap
    List<RemoteEndpointDetails> unmap(in Query query);
    // @see EndpointBinder#unmapFrom
    List<RemoteEndpointDetails> unmapFrom(in String host, in Query query);

    // @see EndpointBinder#setExposed
    void setExposed(in boolean exposed);
    // @see EndpointBinder#setForcable
    void setForcable(in boolean forcable);
}
