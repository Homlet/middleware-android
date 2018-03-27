package uk.ac.cam.seh208.middleware.core;

import junit.framework.Assert;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.JSONSerializable;
import uk.ac.cam.seh208.middleware.common.Polarity;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.core.comms.CloseChannelControlMessage;
import uk.ac.cam.seh208.middleware.core.comms.QueryControlMessage;
import uk.ac.cam.seh208.middleware.core.comms.RemoteEndpointDetails;
import uk.ac.cam.seh208.middleware.core.comms.ControlMessage;
import uk.ac.cam.seh208.middleware.core.comms.OpenChannelsControlMessage;
import uk.ac.cam.seh208.middleware.core.comms.RemoveControlMessage;
import uk.ac.cam.seh208.middleware.core.comms.UpdateControlMessage;
import uk.ac.cam.seh208.middleware.core.exception.InvalidControlMessageException;
import uk.ac.cam.seh208.middleware.core.network.Location;


/**
 * Local test for JSON serialisation of control message objects.
 */
public class ControlMessageTest {

    private static final Random random = new Random(System.nanoTime());

    private static final Query query = new Query.Builder()
            .includeTag("test_tag")
            .setDescRegex("^test_regex.*$")
            .setMatches(5)
            .build();

    private static final RemoteEndpointDetails endpoint = new RemoteEndpointDetails(
            "test",
            "desc",
            Polarity.SOURCE,
            "{}",
            Collections.emptyList(),
            new Location(random.nextLong())
    );

    private static final List<RemoteEndpointDetails> remoteEndpoints = Arrays.asList(
            new RemoteEndpointDetails(
                    "test1",
                    "desc a",
                    Polarity.SOURCE,
                    "{}",
                    Collections.emptyList(),
                    new Location(random.nextLong())
            ),
            new RemoteEndpointDetails(
                    "test2",
                    "desc b",
                    Polarity.SINK,
                    "{}",
                    Arrays.asList("tag1", "tag2"),
                    new Location(random.nextLong())
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

    private static final Location location = new Location(random.nextLong());

    private static final List<Location> locations = Arrays.asList(
            new Location(random.nextLong()),
            new Location(random.nextLong()),
            new Location(random.nextLong())
    );


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
        ControlMessage.Response response = new QueryControlMessage.Response(locations);

        testSerialise(response, ControlMessage.Response.class);
    }

    @Test
    public void testSerialiseUpdate() throws InvalidControlMessageException, IOException {
        ControlMessage message = new UpdateControlMessage(location, endpoints);

        testSerialise(message, ControlMessage.class);
    }

    @Test
    public void testSerialiseUpdateResponse() throws InvalidControlMessageException, IOException {
        ControlMessage.Response response = UpdateControlMessage.Response.getInstance();

        testSerialise(response, ControlMessage.Response.class);
    }

    @Test
    public void testSerialiseRemove() throws InvalidControlMessageException, IOException {
        ControlMessage message = new RemoveControlMessage(location);

        testSerialise(message, ControlMessage.class);
    }

    @Test
    public void testSerialiseRemoveResponse() throws InvalidControlMessageException, IOException {
        ControlMessage.Response response = RemoveControlMessage.Response.getInstance();

        testSerialise(response, ControlMessage.Response.class);
    }
}
