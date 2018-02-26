package uk.ac.cam.seh208.middleware.api.exception;

public class MiddlewareDisconnectedException extends Exception {
    public MiddlewareDisconnectedException() {
        super("Middleware service unexpectedly disconnected.");
    }
}
