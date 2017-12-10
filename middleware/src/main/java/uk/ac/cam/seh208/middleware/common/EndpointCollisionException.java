package uk.ac.cam.seh208.middleware.common;

import android.os.RemoteException;


public class EndpointCollisionException extends RemoteException {
    public EndpointCollisionException(EndpointDetails details) {
        super("Collision creating endpoint with name \"" + details.getName() + "\".");
    }
}
