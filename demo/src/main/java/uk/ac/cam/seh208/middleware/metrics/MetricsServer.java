package uk.ac.cam.seh208.middleware.metrics;

interface MetricsServer {

    void start();

    void stop();

    boolean isStarted();
}
