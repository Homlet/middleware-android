package uk.ac.cam.seh208.middleware.core;

import junit.framework.Assert;

import org.junit.Test;

import java.net.UnknownHostException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import uk.ac.cam.seh208.middleware.core.network.RequestContext;
import uk.ac.cam.seh208.middleware.core.network.RequestStream;
import uk.ac.cam.seh208.middleware.core.network.impl.ZMQAddress;
import uk.ac.cam.seh208.middleware.core.network.impl.ZMQRequestContext;


/**
 * Local test for operation of the ZeroMQ transport classes.
 */
public class ZMQRequestTest {

    @Test
    public void testSimpleRequest() throws InterruptedException, UnknownHostException {
        // Create two ZMQRequestContext objects, with different bound ports.
        int port1 = 8500;
        int port2 = 8501;
        RequestContext context1 = new ZMQRequestContext(port1);
        RequestContext context2 = new ZMQRequestContext(port2);

        // Compute the local address.
        ZMQAddress.Builder addressBuilder = new ZMQAddress.Builder();
        addressBuilder.setHost(ZMQAddress.getLocalHost());

        // Create a request stream between the two contexts.
        RequestStream stream = context1.getRequestStream(addressBuilder.setPort(port2).build());

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
    public void testNoHandler() throws InterruptedException, UnknownHostException {
        // Create two ZMQRequestContext objects, with different bound ports.
        int port1 = 8500;
        int port2 = 8501;
        RequestContext context1 = new ZMQRequestContext(port1);
        RequestContext context2 = new ZMQRequestContext(port2);

        // Compute the local address.
        ZMQAddress.Builder addressBuilder = new ZMQAddress.Builder();
        addressBuilder.setHost(ZMQAddress.getLocalHost());

        // Create a request stream between the two contexts.
        RequestStream stream = context1.getRequestStream(addressBuilder.setPort(port2).build());

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
