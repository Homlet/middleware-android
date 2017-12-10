package uk.ac.cam.seh208.middleware.common;

import android.os.RemoteException;


public class BadHostException extends RemoteException {
    public BadHostException(String host) {
        super("Bad host provided: \"" + host + "\".");
    }
}
