package uk.ac.cam.seh208.middleware.core;

import junit.framework.Assert;

import org.junit.Test;

import java.net.UnknownHostException;
import java.util.HashSet;

import uk.ac.cam.seh208.middleware.core.exception.ConnectionFailedException;
import uk.ac.cam.seh208.middleware.core.exception.MalformedAddressException;
import uk.ac.cam.seh208.middleware.core.comms.Address;
import uk.ac.cam.seh208.middleware.core.comms.Environment;
import uk.ac.cam.seh208.middleware.core.comms.Location;
import uk.ac.cam.seh208.middleware.core.comms.MessageContext;
import uk.ac.cam.seh208.middleware.core.comms.MessageListener;
import uk.ac.cam.seh208.middleware.core.comms.MessageStream;
import uk.ac.cam.seh208.middleware.core.comms.impl.ZMQAddress;
import uk.ac.cam.seh208.middleware.core.comms.impl.ZMQMessageContext;
import uk.ac.cam.seh208.middleware.core.comms.impl.ZMQSchemeConfiguration;


/**
 * Local test for operation of the ZeroMQ transport classes.
 */
public class ZMQMessageTest {

    /**
     * Simple data object storing a closure of a message stream set up
     * for message assertion.
     */
    private static class MessageStreamClosure {
        private MessageStream stream;
        private HashSet<String> seen;
        private MessageListener listener;
        private boolean notified;


        private MessageStreamClosure(MessageStream stream) {
            this.stream = stream;
            seen = new HashSet<>();
            listener = this::seeMessage;
        }

        private MessageStream getStream() {
            return stream;
        }

        private MessageListener getListener() {
            return listener;
        }

        /**
         * Block until the given message is seen on the stream. This should not
         * be called by multiple threads at once.
         *
         * @return whether the message was seen before the timeout.
         */
        private synchronized boolean expectMessage(String message, int timeoutMillis) {
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
        private synchronized void seeMessage(String message) {
            System.out.println("SEEN " + message);
            seen.add(message);
            notified = true;
            notify();
        }
    }


    /**
     * Prepare a stream object for assertion, returning the closure object
     * that should be passed to future assertion calls.
     */
    private static MessageStreamClosure streamSetup(MessageStream stream) {
        MessageStreamClosure closure = new MessageStreamClosure(stream);
        stream.registerListener(closure.getListener());
        return closure;
    }

    /**
     * Remove the assertion listener from a set-up stream object.
     */
    private static void streamClearup(MessageStreamClosure closure) {
        closure.getStream().unregisterListener(closure.getListener());
    }

    /**
     * Block until the specified message is received over a stream. Throw an
     * AssertionFailedError if the message is not received.
     */
    private static void assertRecv(MessageStreamClosure closure, String message,
                                   int timeoutMillis) {
        Assert.assertTrue(closure.expectMessage(message, timeoutMillis));
    }

    /**
     * Attempt to send a message, failing silently if an exception is thrown.
     */
    private static void failsafeSend(MessageStream stream, String message) {
        try {
            stream.send(message);
        } catch (ConnectionFailedException ignored) {
            // Do nothing.
        }
    }

    private static Environment environmentWithPort(int port) {
        return () -> {
            Location location = new Location();
            try {
                location.addAddress(Address.make("zmq://127.0.0.1:" + port));
            } catch (MalformedAddressException e) {
                Assert.fail("Couldn't create environment.");
            }
            return location;
        };
    }

    @Test
    public void testSimpleMessageComms()
            throws InterruptedException, UnknownHostException,
                   ConnectionFailedException, MalformedAddressException {
        // Compute the local addresses.
        int port1 = 8000;
        int port2 = 8001;
        Address address1 = Address.make("zmq://127.0.0.1:" + port1);
        Address address2 = Address.make("zmq://127.0.0.1:" + port2);

        // Create two ZMQMessageContext objects, with different message ports.
        MessageContext context1 = new ZMQMessageContext(
                environmentWithPort(port1), new ZMQSchemeConfiguration(port1));
        MessageContext context2 = new ZMQMessageContext(
                environmentWithPort(port2), new ZMQSchemeConfiguration(port2));

        // Create a message stream between the two contexts.
        MessageStream stream1To2 = context1.getMessageStream(address2);
        MessageStreamClosure closure1 = streamSetup(stream1To2);
        MessageStream stream2To1 = context2.getMessageStream(address1);
        MessageStreamClosure closure2 = streamSetup(stream2To1);

        // Send messages across the two contexts.
        String message1To2 = "1 --> 2";
        String message2To1 = "2 --> 1";
        stream1To2.send(message1To2);
        stream2To1.send(message2To1);

        // Assert that the messages are received.
        assertRecv(closure1, message2To1, 2000);
        assertRecv(closure2, message1To2, 2000);

        // Close the message streams and terminate the contexts.
        streamClearup(closure1);
        stream1To2.close();
        streamClearup(closure2);

        // Check that stream 2 was closed by stream 1's FIN message.
        Thread.sleep(200);
        Assert.assertTrue(stream2To1.isClosed());

        // Terminate the context.
        context1.term();
        context2.term();
    }

    @Test
    public void testComplexMessageComms()
            throws InterruptedException, UnknownHostException, ConnectionFailedException {
        // Create a number of ZMQMessageContext objects, with different message ports.
        int basePort = 9000;
        MessageContext[] contexts = new MessageContext[4];
        for (int i = 0; i < contexts.length; i++) {
            contexts[i] = new ZMQMessageContext(
                    environmentWithPort(basePort + i),
                    new ZMQSchemeConfiguration(basePort + i));
        }

        // Compute the local address.
        ZMQAddress.Builder addressBuilder = new ZMQAddress.Builder();
        addressBuilder.setHost("127.0.0.1");

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

        // Clearup the message streams closures and terminate the contexts.
        streamClearup(closure0);
        streamClearup(closure5);

        // Terminate the contexts.
        for (MessageContext context : contexts) {
            context.term();
        }
    }
}
