package uk.ac.cam.seh208.middleware.common.exception;

import android.os.RemoteException;


public class IncompleteBuildException extends RemoteException {
    public IncompleteBuildException(Class clazz) {
        super("Tried to build an object of type " + clazz.getSimpleName() +
                "from incomplete builder.");
    }
}
