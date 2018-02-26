package uk.ac.cam.seh208.middleware.common.exception;

import android.os.RemoteException;


public class ListenerNotFoundException extends RemoteException {
    public ListenerNotFoundException() {
        super("Could not find the given listener.");
    }
}
