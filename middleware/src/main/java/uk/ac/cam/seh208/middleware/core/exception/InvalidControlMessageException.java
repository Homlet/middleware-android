package uk.ac.cam.seh208.middleware.core.exception;

public class InvalidControlMessageException extends Exception {
    public InvalidControlMessageException(String json) {
        super("Invalid control message JSON string: \"" + json +"\"");
    }
}
