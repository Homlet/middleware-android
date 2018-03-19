package uk.ac.cam.seh208.middleware.core;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.core.comms.ControlMessageHandler;
import uk.ac.cam.seh208.middleware.core.network.Location;
import uk.ac.cam.seh208.middleware.core.network.RequestSwitch;
import uk.ac.cam.seh208.middleware.core.network.impl.ZMQSchemeConfiguration;


public class RDCService extends Service {

    /**
     * Boolean tracking whether the service has previously been started.
     */
    private boolean started;

    /**
     * Switch handling communications at the transport and network layers.
     */
    private RequestSwitch requestSwitch;


    /**
     * Initialise the service fields and set up the communications switch.
     *
     * @return whether the middleware was started.
     */
    private synchronized boolean start() {
        if (started) {
            return false;
        }

        Log.i(getTag(), "RDC starting...");
        Toast.makeText(this, getText(R.string.toast_rdc_starting), Toast.LENGTH_SHORT).show();

        // Initialise object parameters.
        ControlMessageHandler handler = new ControlMessageHandler(this);
        requestSwitch = new RequestSwitch(
                Arrays.asList(
                    new ZMQSchemeConfiguration(ZMQSchemeConfiguration.DEFAULT_RDC_PORT)
                ),
                handler);

        Log.i(getTag(), "RDC started successfully.");
        started = true;
        return true;
    }

    /**
     * Called by Android when the service is started, either from an intent or
     * after a restart attempt.
     *
     * @return START_STICKY to indicate the OS should attempt to restart this
     *         process if it is killed to free resources.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!start()) {
            Log.d(getTag(), "Start command ignored (already started).");
        }

        // Attempt to restart this service if the scheduler kills it for resources.
        return START_STICKY;
    }

    /**
     * Called by Android when a remote process requests to bind to this service.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Attempt to start the service if not already started.
        start();

        // The RDC service has no bound interface.
        return null;
    }

    /**
     * Update the middleware instance record for the given location in the RDC state.
     *
     * @param location Location indexing the middleware instance record.
     * @param details New list of endpoints exposed by that middleware instance.
     */
    public void update(Location location, List<EndpointDetails> details) {
        // TODO: implement.
    }

    /**
     * Remove the middleware instance record for the given location in the RDC state.
     *
     * @param location Location indexing the middleware instance record to remove.
     */
    public void remove(Location location) {
        // TODO: implement.
    }

    private static String getTag() {
        return "RDC";
    }
}
