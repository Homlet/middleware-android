package uk.ac.cam.seh208.middleware.metrics.exception;

public class IncompleteMetricsException extends Exception {
    public IncompleteMetricsException() {
        super("Metrics are not yet complete.");
    }
}
