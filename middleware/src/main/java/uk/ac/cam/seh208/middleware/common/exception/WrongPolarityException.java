package uk.ac.cam.seh208.middleware.common.exception;

import android.os.RemoteException;

import uk.ac.cam.seh208.middleware.common.Polarity;


public class WrongPolarityException extends RemoteException {
    public WrongPolarityException(Polarity polarity) {
        super("Operation not permitted for endpoints of this polarity: " + polarity.toString());
    }
}
