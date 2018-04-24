package uk.ac.cam.seh208.middleware.metrics;

import java8.util.J8Arrays;
import java8.util.stream.StreamSupport;

public class Metrics {

    private final int messages;

    private final int messageLength;

    private final long[] latency;

    private final long[] throughputSend;

    private final long[] throughputRecv;


    /**
     * Construct a new immutable metrics objects from the given data.
     *
     * @param messages The number of messages that were sent in the test.
     * @param latency The round-trip latency for each message, in microseconds.
     * @param throughputSend The sending throughput for each message, in messages per second.
     * @param throughputRecv The receiving throughput for each message, in messages per second.
     * @param messageLength The message length used.
     */
    Metrics(int messages, int messageLength, long[] latency,
            long[] throughputSend, long[] throughputRecv) {
        this.messages = messages;
        this.messageLength = messageLength;
        this.latency = latency;
        this.throughputSend = throughputSend;
        this.throughputRecv = throughputRecv;
    }

    public int getMessages() {
        return messages;
    }

    public int getMessageLength() {
        return messageLength;
    }

    public double getMeanLatency() {
        return J8Arrays.stream(latency)
                .average()
                .orElse(-1.0);
    }

    public double getMedianLatency() {
        return J8Arrays.stream(latency)
                .sorted()
                .skip((messages - 1) / 2)
                .limit(2 - messages % 2)
                .average()
                .orElse(-1.0);
    }

    public double getMeanSendingThroughput() {
        return J8Arrays.stream(throughputSend)
                .skip(1)
                .average()
                .orElse(-1.0);
    }

    public double getMedianSendingThroughput() {
        return J8Arrays.stream(throughputSend)
                .skip(1)
                .sorted()
                .skip((messages - 2) / 2)
                .limit(2 - (messages - 1) % 2)
                .average()
                .orElse(-1.0);
    }

    public double getMeanReceivingThroughput() {
        return J8Arrays.stream(throughputRecv)
                .skip(1)
                .average()
                .orElse(-1.0);
    }

    public double getMedianReceivingThroughput() {
        return J8Arrays.stream(throughputRecv)
                .skip(1)
                .sorted()
                .skip((messages - 2) / 2)
                .limit(2 - (messages - 1) % 2)
                .average()
                .orElse(-1.0);
    }

    public String toCommaSeparatedValues() {
        // Create a new string builder and append the column headings.
        StringBuilder builder = new StringBuilder();
        builder.append("Latency,Sending Throughput,Receiving Throughput\n");

        // Append the values to the string.
        for (int i = 0; i < messages; i++) {
            builder.append(latency[i]);
            builder.append(",");
            builder.append(throughputSend[i]);
            builder.append(",");
            builder.append(throughputRecv[i]);
            builder.append("\n");
        }

        return builder.toString();
    }
}
