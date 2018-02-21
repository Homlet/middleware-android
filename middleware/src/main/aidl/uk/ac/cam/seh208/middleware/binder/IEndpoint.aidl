package uk.ac.cam.seh208.middleware.binder;

import uk.ac.cam.seh208.middleware.common.IMessageListener;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.RemoteEndpointDetails;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.common.Persistence;


interface IEndpoint {
    // @see EndpointBinder#send
    void send(String message);

    // @see EndpointBinder#registerListener
    void registerListener(in IMessageListener listener);
    // @see EndpointBinder#unregisterListener
    void unregisterListener(in IMessageListener listener);
    // @see EndpointBinder#clearListeners
    void clearListeners();

    // @see EndpointBinder#getPeers
    List<RemoteEndpointDetails> getPeers();
    // @see EndpointBinder#map
    List<RemoteEndpointDetails> map(in Query query, in Persistence persistence);
    // @see EndpointBinder#mapTo
    List<RemoteEndpointDetails> mapTo(String remote, in Query query, in Persistence persistence);
    // @see EndpointBinder#unmap
    List<RemoteEndpointDetails> unmap(in Query query);
    // @see EndpointBinder#unmapFrom
    List<RemoteEndpointDetails> unmapFrom(String remote, in Query query);

    // @see EndpointBinder#setExposed
    void setExposed(boolean exposed);
    // @see EndpointBinder#setForceable
    void setForceable(boolean forceable);
}
