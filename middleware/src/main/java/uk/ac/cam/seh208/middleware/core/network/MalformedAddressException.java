package uk.ac.cam.seh208.middleware.core.network;


class MalformedAddressException extends Exception {
    public MalformedAddressException(String string) {
        super("Malformed address string: \"" + string + "\"");
    }
}
