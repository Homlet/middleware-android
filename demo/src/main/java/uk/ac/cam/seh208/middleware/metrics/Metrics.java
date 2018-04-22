package uk.ac.cam.seh208.middleware.metrics;

public class Metrics {

    /**
     * Measure of average round-trip latency in microseconds.
     */
    public final float latency;

    /**
     * Measure of message throughput in messages per second.
     */
    public final float throughput;

    /**
     * The average message string length used to gather these metrics.
     */
    public final int length;


    public Metrics(float latency, float throughput, int length) {
        this.latency = latency;
        this.throughput = throughput;
        this.length = length;
    }
}
