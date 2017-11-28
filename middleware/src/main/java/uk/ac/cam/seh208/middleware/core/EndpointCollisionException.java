package uk.ac.cam.seh208.middleware.core;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;


public class EndpointCollisionException extends Exception {
    public EndpointCollisionException(EndpointDetails details) {
        super("Collision creating endpoint with name \"" + details.getName() + "\".");
    }
}
