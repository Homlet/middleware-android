package uk.ac.cam.seh208.middleware.common.exception;

import android.os.RemoteException;


public class SchemaMismatchException extends RemoteException {
    public SchemaMismatchException() {
        super("Message does not match endpoint schema.");
    }
}
