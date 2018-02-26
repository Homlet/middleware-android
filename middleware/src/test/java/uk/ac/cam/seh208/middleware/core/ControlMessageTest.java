package uk.ac.cam.seh208.middleware.core;

import junit.framework.Assert;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import uk.ac.cam.seh208.middleware.common.JSONSerializable;
import uk.ac.cam.seh208.middleware.common.Polarity;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.core.comms.RemoteEndpointDetails;
import uk.ac.cam.seh208.middleware.core.comms.ControlMessage;
import uk.ac.cam.seh208.middleware.core.comms.OpenChannelsControlMessage;
import uk.ac.cam.seh208.middleware.core.exception.InvalidControlMessageException;
import uk.ac.cam.seh208.middleware.core.network.Location;


/**
 * Local test for operation of the ZeroMQ transport classes.
 */
public class ControlMessageTest {

    @Test
    public void testSerialiseOpenChannels() throws InvalidControlMessageException, IOException {
        Random random = new Random();
        ControlMessage message = new OpenChannelsControlMessage(
                new RemoteEndpointDetails(
                        "test",
                        "desc",
                        Polarity.SOURCE,
                        "{}",
                        Collections.emptyList(),
                        new Location(random.nextInt())
                ),
                new Query.Builder()
                        .includeTag("test_tag")
                        .setDescRegex("^test_regex.*$")
                        .setMatches(5)
                        .build()
        );

        String json = message.toJSON();
        System.out.println(json);

        Assert.assertEquals(message, JSONSerializable.fromJSON(json, ControlMessage.class));
    }

    @Test
    public void testSerialiseOpenChannelsResponse()
            throws InvalidControlMessageException, IOException {
        Random random = new Random();
        ControlMessage.Response response = new OpenChannelsControlMessage.Response(
                Arrays.asList(
                        new RemoteEndpointDetails(
                                "test1",
                                "desc a",
                                Polarity.SOURCE,
                                "{}",
                                Collections.emptyList(),
                                new Location(random.nextInt())
                        ),
                        new RemoteEndpointDetails(
                                "test2",
                                "desc b",
                                Polarity.SINK,
                                "{}",
                                Arrays.asList("tag1", "tag2"),
                                new Location(random.nextInt())
                        )
                )
        );

        String json = response.toJSON();
        System.out.println(json);

        Assert.assertEquals(response,
                JSONSerializable.fromJSON(json, ControlMessage.Response.class));
    }
}
