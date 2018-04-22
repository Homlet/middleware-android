package uk.ac.cam.seh208.middleware.api;

import uk.ac.cam.seh208.middleware.common.IMessageListener;


/**
 * A token returned when registering a message listener with an endpoint.
 * This token may be used in the future to unregister the listener.
 */
public class MessageListenerToken {

    final IMessageListener listener;


    MessageListenerToken(IMessageListener listener) {
        this.listener = listener;
    }
}
