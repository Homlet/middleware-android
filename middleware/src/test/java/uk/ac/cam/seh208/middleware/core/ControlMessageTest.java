package uk.ac.cam.seh208.middleware.core;

import junit.framework.Assert;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import java8.util.Lists;
import uk.ac.cam.seh208.middleware.common.EndpointCommand;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.JSONSerializable;
import uk.ac.cam.seh208.middleware.common.MapToCommand;
import uk.ac.cam.seh208.middleware.common.MiddlewareCommand;
import uk.ac.cam.seh208.middleware.common.Persistence;
import uk.ac.cam.seh208.middleware.common.Polarity;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.common.SetRDCAddressCommand;
import uk.ac.cam.seh208.middleware.core.control.CloseChannelControlMessage;
import uk.ac.cam.seh208.middleware.core.control.EndpointCommandControlMessage;
import uk.ac.cam.seh208.middleware.core.control.Middleware;
import uk.ac.cam.seh208.middleware.core.control.MiddlewareCommandControlMessage;
import uk.ac.cam.seh208.middleware.core.control.QueryControlMessage;
import uk.ac.cam.seh208.middleware.core.control.RemoteEndpointDetails;
import uk.ac.cam.seh208.middleware.core.control.ControlMessage;
import uk.ac.cam.seh208.middleware.core.control.OpenChannelsControlMessage;
import uk.ac.cam.seh208.middleware.core.control.RemoveControlMessage;
import uk.ac.cam.seh208.middleware.core.control.UpdateControlMessage;
import uk.ac.cam.seh208.middleware.core.exception.InvalidControlMessageException;
import uk.ac.cam.seh208.middleware.core.comms.Location;


public class ControlMessageTest {

    private static final Random random = new Random(System.nanoTime());

    private static final Middleware middleware = new Middleware(
            random.nextLong(),
            new Location(),
            new Location());

    private static final List<Middleware> middlewares = Lists.of(middleware);

    private static final Query query = new Query.Builder()
            .includeTag("test_tag")
            .setDescRegex("^test_regex.*$")
            .setMatches(5)
            .build();

    private static final RemoteEndpointDetails endpoint = new RemoteEndpointDetails(
            0,
            "test",
            "desc",
            Polarity.SOURCE,
            "{}",
            Collections.emptyList(),
            middleware
    );

    private static final List<RemoteEndpointDetails> remoteEndpoints = Arrays.asList(
            new RemoteEndpointDetails(
                    0,
                    "test1",
                    "desc a",
                    Polarity.SOURCE,
                    "{}",
                    Collections.emptyList(),
                    middleware
            ),
            new RemoteEndpointDetails(
                    0,
                    "test2",
                    "desc b",
                    Polarity.SINK,
                    "{}",
                    Arrays.asList("tag1", "tag2"),
                    middleware
            )
    );


    private static final List<EndpointDetails> endpoints = Arrays.asList(
            new EndpointDetails(
                    "test1",
                    "desc a",
                    Polarity.SOURCE,
                    "{}",
                    Collections.emptyList()
            ),
            new EndpointDetails(
                    "test2",
                    "desc b",
                    Polarity.SINK,
                    "{}",
                    Arrays.asList("tag1", "tag2")
            )
    );

    private static final MiddlewareCommand mwCommand =
            new SetRDCAddressCommand("zmq://127.0.0.1:4854");

    private static final EndpointCommand epCommand =
            new MapToCommand("zmq://127.0.0.1:4852", query, Persistence.NONE);


    private static void testSerialise(JSONSerializable serializable,
                                      Class<? extends JSONSerializable> clazz) throws IOException {
        String json = serializable.toJSON();
        System.out.println(json);

        Assert.assertEquals(serializable, JSONSerializable.fromJSON(json, clazz));
    }


    @Test
    public void testSerialiseOpenChannels() throws InvalidControlMessageException, IOException {
        ControlMessage message = new OpenChannelsControlMessage(endpoint, query);

        testSerialise(message, ControlMessage.class);
    }

    @Test
    public void testSerialiseOpenChannelsResponse()
            throws InvalidControlMessageException, IOException {
        ControlMessage.Response response = new OpenChannelsControlMessage.Response(remoteEndpoints);

        testSerialise(response, ControlMessage.Response.class);
    }

    @Test
    public void testSerialiseCloseChannel() throws InvalidControlMessageException, IOException {
        ControlMessage message = new CloseChannelControlMessage(random.nextLong());

        testSerialise(message, ControlMessage.class);
    }

    @Test
    public void testSerialiseCloseChannelResponse()
            throws InvalidControlMessageException, IOException {
        ControlMessage.Response response =
                new CloseChannelControlMessage.Response(random.nextBoolean());

        testSerialise(response, ControlMessage.Response.class);
    }

    @Test
    public void testSerialiseQuery() throws InvalidControlMessageException, IOException {
        ControlMessage message = new QueryControlMessage(query);

        testSerialise(message, ControlMessage.class);
    }

    @Test
    public void testSerialiseQueryResponse() throws InvalidControlMessageException, IOException {
        ControlMessage.Response response = new QueryControlMessage.Response(middlewares);

        testSerialise(response, ControlMessage.Response.class);
    }

    @Test
    public void testSerialiseUpdate() throws InvalidControlMessageException, IOException {
        ControlMessage message = new UpdateControlMessage(middleware, endpoints);

        testSerialise(message, ControlMessage.class);
    }

    @Test
    public void testSerialiseUpdateResponse() throws InvalidControlMessageException, IOException {
        ControlMessage.Response response = UpdateControlMessage.Response.getInstance();

        testSerialise(response, ControlMessage.Response.class);
    }

    @Test
    public void testSerialiseRemove() throws InvalidControlMessageException, IOException {
        ControlMessage message = new RemoveControlMessage(middleware);

        testSerialise(message, ControlMessage.class);
    }

    @Test
    public void testSerialiseRemoveResponse() throws InvalidControlMessageException, IOException {
        ControlMessage.Response response = RemoveControlMessage.Response.getInstance();

        testSerialise(response, ControlMessage.Response.class);
    }

    @Test
    public void testSerialiseMiddlewareCommand()
            throws InvalidControlMessageException, IOException {
        ControlMessage message = new MiddlewareCommandControlMessage(mwCommand);

        testSerialise(message, ControlMessage.class);
    }

    @Test
    public void testSerialiseMiddlewareCommandResponse()
            throws InvalidControlMessageException, IOException {
        ControlMessage.Response response =
                new MiddlewareCommandControlMessage.Response(random.nextBoolean());

        testSerialise(response, ControlMessage.Response.class);
    }

    @Test
    public void testSerialiseEndpointCommand()
            throws InvalidControlMessageException, IOException {
        ControlMessage message = new EndpointCommandControlMessage("test", epCommand);

        testSerialise(message, ControlMessage.class);
    }

    @Test
    public void testSerialiseEndpointCommandResponse()
            throws InvalidControlMessageException, IOException {
        ControlMessage.Response response =
                new EndpointCommandControlMessage.Response(random.nextBoolean());

        testSerialise(response, ControlMessage.Response.class);
    }
}
