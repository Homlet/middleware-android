package uk.ac.cam.seh208.middleware.api;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.Polarity;


/**
 * Instrumented test for the correctness of binding to the middleware.
 */
public class InterfaceTest {

    private static Middleware middleware;


    @BeforeClass
    public static void bind() throws Exception {
        Context context = InstrumentationRegistry.getContext();
        middleware = new Middleware(context);
    }

    @Test
    public void testCreateSource() throws Exception {
        // Store the defined endpoint details from before the test.
        Set<EndpointDetails> detailsBefore = new HashSet<>(middleware.getAllEndpointDetails());

        // Define some endpoint details.
        String name = "test_src";
        String desc = "A test source endpoint, which emits strings.";
        String schema = "{" +
                "\"$schema\": \"http://json-schema.org/schema#\"," +
                "\"type\": \"string\" " +
                "}";
        List<String> tags = Arrays.asList("string", "temporary");

        // Create a new source endpoint with those details.
        middleware.createSource(name, desc, schema, tags, true, true);

        // Get the endpoint back from the middleware.
        Endpoint endpoint = middleware.getEndpoint(name);
        EndpointDetails details = endpoint.getDetails();

        // Check that the stored details match.
        Assert.assertEquals(name, details.getName());
        Assert.assertEquals(desc, details.getDesc());
        Assert.assertEquals(schema, details.getSchema());
        Assert.assertEquals(new HashSet<>(tags), new HashSet<>(details.getTags()));
        Assert.assertEquals(Polarity.SOURCE, details.getPolarity());

        // Destroy the endpoint.
        middleware.destroyEndpoint(name);
        details = middleware.getEndpointDetails(name);
        Assert.assertNull(details);

        // Check that the endpoint details set matches the stored set.
        Assert.assertEquals(detailsBefore, new HashSet<>(middleware.getAllEndpointDetails()));
    }

    @Test
    public void testCreateSink() throws Exception {
        // Store the defined endpoint details from before the test.
        Set<EndpointDetails> detailsBefore = new HashSet<>(middleware.getAllEndpointDetails());

        // Define some endpoint details.
        String name = "test_snk";
        String desc = "A test sink endpoint, which accepts strings.";
        String schema = "{" +
                            "\"$schema\": \"http://json-schema.org/schema#\"," +
                            "\"type\": \"string\" " +
                        "}";
        List<String> tags = Arrays.asList("string", "temporary");

        // Create a new sink endpoint with those details.
        middleware.createSink(name, desc, schema, tags, true, true);

        // Get the endpoint back from the middleware.
        Endpoint endpoint = middleware.getEndpoint(name);
        EndpointDetails details = endpoint.getDetails();

        // Check that the stored details match.
        Assert.assertEquals(name, details.getName());
        Assert.assertEquals(desc, details.getDesc());
        Assert.assertEquals(schema, details.getSchema());
        Assert.assertEquals(new HashSet<>(tags), new HashSet<>(details.getTags()));
        Assert.assertEquals(Polarity.SINK, details.getPolarity());

        // Destroy the endpoint.
        middleware.destroyEndpoint(name);
        details = middleware.getEndpointDetails(name);
        Assert.assertNull(details);

        // Check that the endpoint details set matches the stored set.
        Assert.assertEquals(detailsBefore, new HashSet<>(middleware.getAllEndpointDetails()));
    }
}
