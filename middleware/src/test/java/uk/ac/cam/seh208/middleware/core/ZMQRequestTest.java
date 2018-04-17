package uk.ac.cam.seh208.middleware.core;

import junit.framework.Assert;

import org.junit.Test;

import java.net.UnknownHostException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import uk.ac.cam.seh208.middleware.core.exception.MalformedAddressException;
import uk.ac.cam.seh208.middleware.core.comms.Address;
import uk.ac.cam.seh208.middleware.core.comms.RequestContext;
import uk.ac.cam.seh208.middleware.core.comms.RequestStream;
import uk.ac.cam.seh208.middleware.core.comms.impl.ZMQRequestContext;
import uk.ac.cam.seh208.middleware.core.comms.impl.ZMQSchemeConfiguration;


/**
 * Local test for operation of the ZeroMQ transport classes.
 */
public class ZMQRequestTest {

    @Test
    public void testSimpleRequest()
            throws InterruptedException, UnknownHostException, MalformedAddressException {
        // Create two ZMQRequestContext objects, with different bound ports.
        int port1 = 8500;
        int port2 = 8501;
        RequestContext context1 = new ZMQRequestContext(new ZMQSchemeConfiguration(port1));
        RequestContext context2 = new ZMQRequestContext(new ZMQSchemeConfiguration(port2));

        // Compute the local address.
        Address address = Address.make("zmq://127.0.0.1:" + port2);

        // Create a request stream between the two contexts.
        RequestStream stream = context1.getRequestStream(address);

        // Set up the responder.
        context2.getResponder().setHandler(request -> request + "!!!");

        // Send a random request over the stream.
        Random random = new Random(System.nanoTime());
        String request = String.valueOf(random.nextLong());
        String response = stream.request(request);

        // Assert that the correct response was received.
        Assert.assertEquals(request + "!!!", response);

        // Close the request stream and terminate the contexts.
        stream.close();

        // Catch up on logging.
        Thread.sleep(500);

        // Terminate the context.
        context1.term();
        context2.term();
    }

    @Test
    public void testNoHandler()
            throws InterruptedException, UnknownHostException, MalformedAddressException {
        // Create two ZMQRequestContext objects, with different bound ports.
        int port1 = 8500;
        int port2 = 8501;
        RequestContext context1 = new ZMQRequestContext(new ZMQSchemeConfiguration(port1));
        RequestContext context2 = new ZMQRequestContext(new ZMQSchemeConfiguration(port2));

        // Compute the local address.
        Address address = Address.make("zmq://127.0.0.1:" + port2);

        // Create a request stream between the two contexts.
        RequestStream stream = context1.getRequestStream(address);

        new Timer().schedule(new TimerTask() {
                                 @Override
                                 public void run() {
                                     // Set up the responder.
                                     context2.getResponder().setHandler(request -> request + "!!!");
                                 }
                             }, 500);

        // Send a random request over the stream.
        Random random = new Random(System.nanoTime());
        String request = String.valueOf(random.nextLong());
        String response = stream.request(request);

        // Assert that the correct response was received.
        Assert.assertEquals(request + "!!!", response);

        // Close the request stream and terminate the contexts.
        stream.close();

        // Catch up on logging.
        Thread.sleep(500);

        // Terminate the context.
        context1.term();
        context2.term();
    }
}
