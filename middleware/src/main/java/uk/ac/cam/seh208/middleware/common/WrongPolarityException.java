package uk.ac.cam.seh208.middleware.common;

import android.os.RemoteException;


public class WrongPolarityException extends RemoteException {
    public WrongPolarityException(Polarity polarity) {
        super("Operation not permitted for endpoints of this polarity: " + polarity.toString());
    }
}
