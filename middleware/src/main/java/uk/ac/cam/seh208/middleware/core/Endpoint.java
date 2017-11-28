package uk.ac.cam.seh208.middleware.core;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.Polarity;


/**
 * Object encapsulating the state of an active
 */
public class Endpoint {
    private EndpointDetails details;


    public Endpoint(EndpointDetails details) {
        this.details = details;
    }

    public void initialise() {
        // TODO: implement.
    }

    public void destroy() {
        // TODO: implement.
    }

    /**
     * Convenience method for getting the endpoint name from the details.
     *
     * @return the endpoint name.
     */
    public String getName() {
        return details.getName();
    }

    /**
     * Convenience method for getting the endpoint polarity from the details.
     *
     * @return the endpoint polarity.
     */
    public Polarity getPolarity() {
        return details.getPolarity();
    }

    /**
     * @return a reference to the immutable endpoint details object.
     */
    public EndpointDetails getDetails() {
        return details;
    }
}
