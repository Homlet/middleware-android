package uk.ac.cam.seh208.middleware.common.exception;

import android.os.RemoteException;


public class SchemaMismatchException extends RemoteException {
    public SchemaMismatchException(String message, String schema) {
        super("Message \"" + message + "\" does not match endpoint schema \"" + schema + "\"");
    }
}
