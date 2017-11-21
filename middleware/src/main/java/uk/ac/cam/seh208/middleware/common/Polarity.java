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
    SOURCE,

    /**
     * Data sink endpoints listen for data from peered sources, and pass
     * it to all applications having registered interest via a callback.
     */
    SINK
}
