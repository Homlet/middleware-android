package uk.ac.cam.seh208.middleware.common;

interface IMessageListener {
    oneway void onMessage(in String message);
}
