package uk.ac.cam.seh208.middleware.common;

import android.os.RemoteException;


public class BadQueryException extends RemoteException {
    public BadQueryException(Query query) {
        super("Invalid query: " + query);
    }
}
