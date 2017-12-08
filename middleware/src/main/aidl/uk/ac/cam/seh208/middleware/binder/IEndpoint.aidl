package uk.ac.cam.seh208.middleware.binder;

import uk.ac.cam.seh208.middleware.common.IMessageListener;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.RemoteEndpointDetails;
import uk.ac.cam.seh208.middleware.common.Query;


interface IEndpoint {
    // @see EndpointBinder#send
    void send(in String message);

    // @see EndpointBinder#registerListener
    void registerListener(in IMessageListener listener);
    // @see EndpointBinder#unregisterListener
    void unregisterListener(in IMessageListener listener);
    // @see EndpointBinder#clearListeners
    void clearListeners();

    // TODO: work persistence into mapping. Use three levels: none, resend-query, perfect-match.
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
    // @see EndpointBinder#setForceable
    void setForceable(in boolean forceable);
}
