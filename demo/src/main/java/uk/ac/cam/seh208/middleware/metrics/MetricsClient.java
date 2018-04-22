package uk.ac.cam.seh208.middleware.metrics;

import uk.ac.cam.seh208.middleware.metrics.exception.IncompleteMetricsException;

public interface MetricsClient {

    Metrics runMetrics(int messages, int length) throws IncompleteMetricsException;
}
