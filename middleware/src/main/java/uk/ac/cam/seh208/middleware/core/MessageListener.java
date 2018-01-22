package uk.ac.cam.seh208.middleware.core;


public interface MessageListener {
    void onMessage(String message, long channelId);
}
