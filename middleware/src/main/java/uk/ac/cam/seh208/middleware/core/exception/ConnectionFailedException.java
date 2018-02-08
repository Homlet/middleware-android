package uk.ac.cam.seh208.middleware.core.exception;

import uk.ac.cam.seh208.middleware.core.network.Address;

public class ConnectionFailedException extends Exception {
    public ConnectionFailedException(Address address) {
        super("Connection to remote host on address \"" + address + "\" failed.");
    }
}
