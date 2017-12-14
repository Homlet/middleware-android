package uk.ac.cam.seh208.middleware.common.exception;

import android.os.RemoteException;


public class InvalidSchemaException extends RemoteException {
    public InvalidSchemaException(String schema) {
        super("Invalid message schema provided: \"" + schema + "\"");
    }
}
