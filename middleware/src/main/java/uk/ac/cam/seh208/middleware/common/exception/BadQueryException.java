package uk.ac.cam.seh208.middleware.common.exception;

import android.os.RemoteException;

import uk.ac.cam.seh208.middleware.common.Query;


public class BadQueryException extends RemoteException {
    public BadQueryException(Query query) {
        super("Invalid query: " + query);
    }
}
