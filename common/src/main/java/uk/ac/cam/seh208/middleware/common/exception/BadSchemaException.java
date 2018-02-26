package uk.ac.cam.seh208.middleware.common.exception;

import android.os.RemoteException;


public class BadSchemaException extends RemoteException {
    public BadSchemaException(String schema) {
        super("Invalid message schema provided: \"" + schema + "\"");
    }
}
