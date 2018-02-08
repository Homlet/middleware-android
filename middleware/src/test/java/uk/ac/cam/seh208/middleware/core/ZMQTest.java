package uk.ac.cam.seh208.middleware.core;

import junit.framework.Assert;

import org.junit.Test;

import java.net.UnknownHostException;
import java.util.HashSet;

import uk.ac.cam.seh208.middleware.core.exception.ConnectionFailedException;
import uk.ac.cam.seh208.middleware.core.network.MessageContext;
import uk.ac.cam.seh208.middleware.core.network.MessageListener;
import uk.ac.cam.seh208.middleware.core.network.MessageStream;
import uk.ac.cam.seh208.middleware.core.network.impl.ZMQAddress;
import uk.ac.cam.seh208.middleware.core.network.impl.ZMQContext;


/**
 * Local test for operation of the ZeroMQ transport classes.
 */
public class ZMQTest {

    /**
     * Simple data object storing a closure of a message stream set up
     * for message assertion.
     */
    private static class MessageStreamClosure {
        private MessageStream stream;
        private HashSet<String> seen;
        private MessageListener listener;
        private boolean notified;


        public MessageStreamClosure(MessageStream stream) {
            this.stream = stream;
            seen = new HashSet<>();
            listener = this::seeMessage;
        }

        public MessageStream getStream() {
            return stream;
        }

        public MessageListener getListener() {
            return listener;
        }

        /**
         * Block until the given message is seen on the stream. This should not
         * be called by multiple threads at once.
         *
         * @return whether the message was seen before the timeout.
         */
        public synchronized boolean expectMessage(String message, int timeoutMillis) {
            if (message == null) {
                // We won't see null messages.
                return false;
            }

            try {
                // Wait until the expected message is seen.
                notified = true;
                while (notified && !seen.contains(message)) {
                    notified = false;
                    wait(timeoutMillis);
                }

                if (notified) {
                    // The expected message was seen; remove it from the seen list
                    // in case we wait on the same message again.
                    seen.remove(message);
                    notified = false;
                    return true;
                } else {
                    // We timed out.
                    return false;
                }
            } catch (InterruptedException e) {
                // We timed out.
                return false;
            }
        }

        /**
         * Notify the waiting thread of a seen message.
         */
        public synchronized void seeMessage(String message) {
            seen.add(message);
            notified = true;
            notify();
        }
    }


    /**
     * Prepare a stream object for assertion, returning the closure object
     * that should be passed to future assertion calls.
     */
    public static MessageStreamClosure streamSetup(MessageStream stream) {
        MessageStreamClosure closure = new MessageStreamClosure(stream);
        stream.registerListener(closure.getListener());
        return closure;
    }

    /**
     * Remove the assertion listener from a set-up stream object.
     */
    public static void streamClearup(MessageStreamClosure closure) {
        closure.getStream().unregisterListener(closure.getListener());
    }

    /**
     * Block until the specified message is received over a stream. Throw an
     * AssertionFailedError if the message is not received.
     */
    public static void assertRecv(MessageStreamClosure closure, String message, int timeoutMillis) {
        Assert.assertTrue(closure.expectMessage(message, timeoutMillis));
    }

    /**
     * Attempt to send a message, failing silently if an exception is thrown.
     */
    public static void failsafeSend(MessageStream stream, String message) {
        try {
            stream.send(message);
        } catch (ConnectionFailedException ignored) {
            // Do nothing.
        }
    }

    @Test
    public void testSimpleMessageComms() throws InterruptedException, UnknownHostException, ConnectionFailedException {
        // Create two ZMQContext objects, with different message ports.
        int port1 = 8000;
        int port2 = 8001;
        MessageContext context1 = new ZMQContext(port1, 0);
        MessageContext context2 = new ZMQContext(port2, 0);

        // Compute the local address.
        ZMQAddress.Builder addressBuilder = new ZMQAddress.Builder();
        addressBuilder.setHost(ZMQContext.getLocalHost());

        // Create a message stream between the two contexts.
        MessageStream stream1 = context1.getMessageStream(addressBuilder.setPort(port2).build());
        MessageStreamClosure closure1 = streamSetup(stream1);
        MessageStream stream2 = context2.getMessageStream(addressBuilder.setPort(port1).build());
        MessageStreamClosure closure2 = streamSetup(stream2);

        // Send messages across the two contexts.
        String message1To2 = "1 --> 2";
        String message2To1 = "2 --> 1";
        stream1.send(message1To2);
        stream2.send(message2To1);

        // Assert that the messages are received.
        assertRecv(closure1, message2To1, 2000);
        assertRecv(closure2, message1To2, 2000);

        // Close the message streams and terminate the contexts.
        streamClearup(closure1);
        stream1.close();
        streamClearup(closure2);

        // Check that stream 2 was closed by stream 1's FIN message.
        Thread.sleep(500);
        Assert.assertTrue(stream2.isClosed());

        // TODO: terminate contexts.
    }

    @Test
    public void testComplexMessageComms() throws InterruptedException, UnknownHostException, ConnectionFailedException {
        // Create a number of ZMQContext objects, with different message ports.
        int basePort = 9000;
        MessageContext[] contexts = new MessageContext[4];
        for (int i = 0; i < contexts.length; i++) {
            contexts[i] = new ZMQContext(basePort + i, 0);
        }

        // Compute the local address.
        ZMQAddress.Builder addressBuilder = new ZMQAddress.Builder();
        addressBuilder.setHost(ZMQContext.getLocalHost());

        // Create a forwarding network of message streams between the contexts.
        MessageStream[] streams = new MessageStream[6];
        streams[0] = contexts[0].getMessageStream(addressBuilder.setPort(basePort + 1).build());
        streams[1] = contexts[1].getMessageStream(addressBuilder.setPort(basePort).build());
        streams[2] = contexts[1].getMessageStream(addressBuilder.setPort(basePort + 2).build());
        streams[3] = contexts[2].getMessageStream(addressBuilder.setPort(basePort + 1).build());
        streams[4] = contexts[2].getMessageStream(addressBuilder.setPort(basePort + 3).build());
        streams[5] = contexts[3].getMessageStream(addressBuilder.setPort(basePort + 2).build());

        // Set up message listeners and closures.
        MessageStreamClosure closure0 = streamSetup(streams[0]);
        streams[1].registerListener(message -> failsafeSend(streams[2], message));
        streams[2].registerListener(message -> failsafeSend(streams[1], message));
        streams[3].registerListener(message -> failsafeSend(streams[4], message));
        streams[4].registerListener(message -> failsafeSend(streams[3], message));
        MessageStreamClosure closure5 = streamSetup(streams[5]);

        // Send messages from the endpoints.
        String message0To5 = "0 --> 5";
        String message5To0 = "5 --> 0";
        streams[0].send(message0To5);
        streams[5].send(message5To0);

        // Assert that the messages are received.
        assertRecv(closure0, message5To0, 2000);
        assertRecv(closure5, message0To5, 2000);

        // Close the message streams and terminate the contexts.
        streamClearup(closure0);
        streamClearup(closure5);
        for (MessageStream stream : streams) {
            stream.close();
        }

        // Let logging catch up.
        Thread.sleep(500);

        // TODO: terminate contexts.
    }
}
