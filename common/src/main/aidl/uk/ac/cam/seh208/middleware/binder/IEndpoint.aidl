package uk.ac.cam.seh208.middleware.binder;

import uk.ac.cam.seh208.middleware.common.IMessageListener;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
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

    // @see EndpointBinder#map
    long map(in Query query, in Persistence persistence);
    // @see EndpointBinder#unmap
    void unmap(long mappingId);
    // @see EndpointBinder#unmapAll
    void unmapAll();
    // @see EndpointBinder#close
    int close(in Query query);
    // @see EndpointBinder#closeAll
    int closeAll();

    // @see EndpointBinder#setExposed
    void setExposed(boolean exposed);
    // @see EndpointBinder#setForceable
    void setForceable(boolean forceable);
}
