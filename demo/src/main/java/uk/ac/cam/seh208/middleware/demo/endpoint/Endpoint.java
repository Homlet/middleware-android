package uk.ac.cam.seh208.middleware.demo.endpoint;


// TODO: move this definition within the AIDL interface of the middleware itself.
public class Endpoint {

    /**
     * Enumeration of supported endpoint polarities; these describe the direction
     * in which data flows through the endpoint.
     */
    public enum Polarity {
        /**
         * Data source endpoints emit data from applications, forwarding it
         * to all peer sinks.
         */
        SOURCE,

        /**
         * Data sink endpoints listen for data on peer sources, and forward
         * it to all applications having registered interest via a callback.
         */
        SINK
    }


    /**
     * The canonical name of this endpoint within the middleware instance.
     * This name is used to uniquely identify the endpoint between different
     * applications using the middleware.
     */
    private String cname;

    /**
     * A human-readable desc of the purpose of the endpoint.
     */
    private String desc;

    /**
     * The direction in which data moves within the endpoint.
     */
    private Polarity polarity;

    // TODO: include representation of schema.


    public Endpoint(String cname, String desc, Polarity polarity) {
        // TODO: register endpoint with the middleware.

        this.cname = cname;
        this.desc = desc;
        this.polarity = polarity;
    }

    public String getCName() {
        return cname;
    }

    public String getDesc() {
        return desc;
    }

    public Polarity getPolarity() {
        return polarity;
    }
}
