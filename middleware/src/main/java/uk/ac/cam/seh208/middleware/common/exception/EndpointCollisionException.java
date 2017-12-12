package uk.ac.cam.seh208.middleware.common.exception;

import android.os.RemoteException;


public class EndpointCollisionException extends RemoteException {
    public EndpointCollisionException(String name) {
        super("Collision creating endpoint with name \"" + name + "\".");
    }
}
