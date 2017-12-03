package uk.ac.cam.seh208.middleware.core;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.Polarity;


/**
 * Object encapsulating the state of an active
 */
public class Endpoint {
    private EndpointDetails details;
    private boolean exposed;
    private boolean forceable;


    public Endpoint(EndpointDetails details, boolean exposed, boolean forceable) {
        this.details = details;
        this.exposed = exposed;
        this.forceable = forceable;
    }

    public Endpoint(EndpointDetails details) {
        this(details, true, true);
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

    public EndpointDetails getDetails() {
        return details;
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    public boolean isForceable() {
        return forceable;
    }

    public void setForceable(boolean forceable) {
        this.forceable = forceable;
    }
}
