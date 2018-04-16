package uk.ac.cam.seh208.middleware.core.exception;

public class NoValidAddressException extends Exception {
    public NoValidAddressException() {
        super("No valid addresses are stored in the location.");
    }
}
