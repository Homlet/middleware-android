package uk.ac.cam.seh208.middleware.common;


/**
 * Enumeration of supported endpoint polarities; these describe the direction
 * in which data flows through any particular endpoint.
 */
public enum Polarity {
    /**
     * Data source endpoints emit data from applications, forwarding it
     * to all peered sinks.
     */
    SOURCE(true, false),

    /**
     * Data sink endpoints listen for data from peered sources, and pass
     * it to all applications having registered interest via a callback.
     */
    SINK(false, true);


    /**
     * Indicates whether messages may be sent from endpoints of this polarity.
     */
    public final boolean supportsSending;

    /**
     * Indicates whether incoming message listeners may be attached to
     * endpoints of this polarity.
     */
    public final boolean supportsListeners;


    Polarity(boolean supportsSending, boolean supportsListeners) {
        this.supportsSending = supportsSending;
        this.supportsListeners = supportsListeners;
    }
}
