package uk.ac.cam.seh208.middleware.common;

import android.os.RemoteException;


public class ProtocolException extends RemoteException {
    public ProtocolException() {
        super("Remote component broke protocol.");
    }
}
