package uk.ac.cam.seh208.middleware.core.exception;


public class MalformedAddressException extends Exception {
    public MalformedAddressException(String string) {
        super("Malformed address string: \"" + string + "\"");
    }
}
