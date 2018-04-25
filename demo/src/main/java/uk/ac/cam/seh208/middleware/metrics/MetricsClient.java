package uk.ac.cam.seh208.middleware.metrics;

import uk.ac.cam.seh208.middleware.metrics.exception.IncompleteMetricsException;

public interface MetricsClient {

    Metrics runMetrics(int messages, int length) throws IncompleteMetricsException;

    String getName();

    /**
     * @return the minimum message length as is dependent on the maximum sequence number.
     */
    static int minLength(int messages) {
        // Messages are formatted as JSON lists: [seq,"padding..."]. Therefore, there
        // is a minimum length dependent on the maximum sequence number.
        return (int) Math.ceil(Math.log10(messages)) + 5;
    }

    /**
     * Perform standard analyses on message timing data to produce a metrics object.
     *
     * @param timeSend Array of times at which each message was sent (in nanoseconds).
     * @param timeRecv Array of times at which each message was received (in nanoseconds).
     * @param length The length of each message.
     */
    static Metrics process(long[] timeSend, long[] timeRecv, int length) {
        int messages = timeSend.length;
        int period = 5;

        // Calculate the round-trip latency.
        long[] latency = new long[messages];
        for (int i = 0; i < messages; i++) {
            latency[i] = (timeRecv[i] - timeSend[i]) / 1000;
        }

        // Calculate the sending and receiving throughputs.
        long[] throughputSend = new long[messages];
        long[] throughputRecv = new long[messages];
        for (int i = period; i < messages; i++) {
            if (timeSend[i] - timeSend[i-period] != 0) {
                throughputSend[i] =
                        ((long) period * 1000000000L) / (timeSend[i] - timeSend[i - period]);
            }
            if (timeRecv[i] - timeRecv[i-period] != 0) {
                throughputRecv[i] =
                        ((long) period * 1000000000L) / (timeRecv[i] - timeRecv[i - period]);
            }
        }

        return new Metrics(messages, length, latency, throughputSend, throughputRecv);
    }
}
