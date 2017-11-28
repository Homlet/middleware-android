package uk.ac.cam.seh208.middleware.core;


public class EndpointNotFoundException extends Exception {
    public EndpointNotFoundException(String name) {
        super("Could not find endpoint with name \"" + name + "\".");
    }
}
