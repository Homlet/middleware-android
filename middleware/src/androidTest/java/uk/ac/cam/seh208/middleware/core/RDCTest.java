package uk.ac.cam.seh208.middleware.core;

import android.content.Intent;
import android.os.IBinder;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.TimeoutException;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.Polarity;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.common.exception.BadHostException;
import uk.ac.cam.seh208.middleware.common.exception.BadSchemaException;
import uk.ac.cam.seh208.middleware.common.exception.EndpointCollisionException;
import uk.ac.cam.seh208.middleware.core.network.Address;
import uk.ac.cam.seh208.middleware.core.network.Location;
import uk.ac.cam.seh208.middleware.core.network.impl.ZMQAddress;
import uk.ac.cam.seh208.middleware.core.network.impl.ZMQSchemeConfiguration;


@RunWith(AndroidJUnit4.class)
public class RDCTest {

    @Rule
    public final ServiceTestRule mwServiceRule = new ServiceTestRule();

    @Rule
    public final ServiceTestRule rdcServiceRule = new ServiceTestRule();

    private MiddlewareService middleware;

    private RDCService rdc;


    @Before
    public void bind() throws TimeoutException, EndpointCollisionException,
                              BadSchemaException, InterruptedException {
        // Start and bind to the services.
        Intent mwIntent = new Intent(
                InstrumentationRegistry.getTargetContext(),
                TestMiddlewareService.class);
        Intent rdcIntent = new Intent(
                InstrumentationRegistry.getTargetContext(),
                TestRDCService.class);

        IBinder mwBinder = mwServiceRule.bindService(mwIntent);
        IBinder rdcBinder = rdcServiceRule.bindService(rdcIntent);

        middleware = ((TestMiddlewareService.LocalBinder) mwBinder).getService();
        rdc = ((TestRDCService.LocalBinder) rdcBinder).getService();

        // Configure the services.
        Address rdcAddress = new ZMQAddress.Builder()
                .setHost("localhost")
                .setPort(ZMQSchemeConfiguration.DEFAULT_RDC_PORT)
                .build();
        Location rdcLocation = new Location(-1);
        rdcLocation.addAddress(rdcAddress);
        middleware.setRDCLocation(rdcLocation);

        // Create some endpoints.
        middleware.createEndpoint(
                new EndpointDetails("test1", "A test endpoint.", Polarity.SOURCE, "{}", null),
                true,
                true);
        middleware.createEndpoint(
                new EndpointDetails("test2", "A test endpoint.", Polarity.SOURCE, "{}", null),
                true,
                true);
        middleware.createEndpoint(
                new EndpointDetails("test3", "A hidden test endpoint.", Polarity.SINK, "{}", null),
                false,
                true);

        // Wait for the middleware to update with the RDC.
        Thread.sleep(MiddlewareService.RDC_DELAY_MILLIS);
    }

    @Test
    public void testDiscoverTrivial() throws BadHostException {
        // Build a query matching all endpoints.
        Query query = new Query.Builder().build();

        // Discover via the middleware.
        Assert.assertEquals(middleware.getLocation(), middleware.discover(query));
    }

    @Test
    public void testDiscoverSuccess() throws BadHostException {
        // Build a query matching test1 and test2.
        Query query = new Query.Builder()
                .setPolarity(Polarity.SOURCE)
                .setSchema("{}")
                .build();

        // Discover via the middleware.
        Assert.assertEquals(middleware.getLocation(), middleware.discover(query));
    }

    @Test
    public void testDiscoverFailDueToNoCandidates() throws BadHostException {
        // Build a query matching no endpoints.
        Query query = new Query.Builder()
                .setSchema("{\"type\": \"string\"}")
                .build();

        // Discover via the middleware.
        Assert.assertEquals(Collections.emptyList(), middleware.discover(query));
    }

    @Test
    public void testDiscoverFailDueToHiddenCandidates() throws BadHostException {
        // Build a query matching test3 only.
        Query query = new Query.Builder()
                .setPolarity(Polarity.SINK)
                .build();

        // Discover via the middleware.
        Assert.assertEquals(Collections.emptyList(), middleware.discover(query));
    }

    @After
    public void stop() {
        mwServiceRule.unbindService();
        rdcServiceRule.unbindService();
    }
}
