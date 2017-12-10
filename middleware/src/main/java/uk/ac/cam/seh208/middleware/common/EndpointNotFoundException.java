package uk.ac.cam.seh208.middleware.common;

import android.os.RemoteException;


public class EndpointNotFoundException extends RemoteException {
    public EndpointNotFoundException(String name) {
        super("Could not find endpoint with name \"" + name + "\".");
    }
}
