package uk.ac.cam.seh208.middleware.common;

import android.os.RemoteException;


public class SchemaMismatchException extends RemoteException {
    public SchemaMismatchException() {
        super("Message does not match endpoint schema.");
    }
}
