package uk.ac.cam.seh208.middleware.common.exception;

import android.os.RemoteException;


public class MappingNotFoundException extends RemoteException {
    public MappingNotFoundException(long mappingId) {
        super("Could not find mapping with identifier (" + mappingId + ")");
    }
}
