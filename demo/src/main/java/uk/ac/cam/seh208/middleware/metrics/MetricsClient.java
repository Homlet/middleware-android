package uk.ac.cam.seh208.middleware.metrics;

import uk.ac.cam.seh208.middleware.metrics.exception.IncompleteMetricsException;

public interface MetricsClient {

    void connect();

    void disconnect();

    void runLatency(int messages, int delayMillis);

    void runThroughput(int messages);

    Metrics getMetrics() throws IncompleteMetricsException;
}
