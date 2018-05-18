package uk.ac.cam.seh208.middleware.core;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java8.util.stream.StreamSupport;

import uk.ac.cam.seh208.middleware.binder.CombinedBinder;
import uk.ac.cam.seh208.middleware.common.EndpointCommand;
import uk.ac.cam.seh208.middleware.common.MiddlewareCommand;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.common.SetRDCAddressCommand;
import uk.ac.cam.seh208.middleware.core.control.EndpointCommandControlMessage;
import uk.ac.cam.seh208.middleware.core.control.Middleware;
import uk.ac.cam.seh208.middleware.core.control.MiddlewareCommandControlMessage;
import uk.ac.cam.seh208.middleware.core.control.MiddlewareDatabase;
import uk.ac.cam.seh208.middleware.core.control.QueryControlMessage;
import uk.ac.cam.seh208.middleware.core.control.RemoteEndpointDetails;
import uk.ac.cam.seh208.middleware.common.exception.BadHostException;
import uk.ac.cam.seh208.middleware.common.exception.EndpointCollisionException;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.exception.EndpointNotFoundException;
import uk.ac.cam.seh208.middleware.common.exception.BadSchemaException;
import uk.ac.cam.seh208.middleware.core.control.ControlMessageHandler;
import uk.ac.cam.seh208.middleware.core.control.Endpoint;
import uk.ac.cam.seh208.middleware.core.control.EndpointSet;
import uk.ac.cam.seh208.middleware.core.control.Multiplexer;
import uk.ac.cam.seh208.middleware.core.control.MultiplexerPool;
import uk.ac.cam.seh208.middleware.core.control.RemoveControlMessage;
import uk.ac.cam.seh208.middleware.core.control.UpdateControlMessage;
import uk.ac.cam.seh208.middleware.core.exception.MalformedAddressException;
import uk.ac.cam.seh208.middleware.core.exception.NoValidAddressException;
import uk.ac.cam.seh208.middleware.core.exception.UnexpectedClosureException;
import uk.ac.cam.seh208.middleware.core.comms.Address;
import uk.ac.cam.seh208.middleware.core.comms.Location;
import uk.ac.cam.seh208.middleware.core.comms.MessageStream;
import uk.ac.cam.seh208.middleware.core.comms.MessageSwitch;
import uk.ac.cam.seh208.middleware.core.comms.RequestStream;
import uk.ac.cam.seh208.middleware.core.comms.RequestSwitch;
import uk.ac.cam.seh208.middleware.core.comms.impl.ZMQSchemeConfiguration;


public class MiddlewareService extends Service {

    /**
     * The delay in seconds between RDC update ticks.
     */
    public static final int RDC_DELAY_MILLIS = 5000;

    /**
     * The amount of time waited for an acknowledgement from the RDC before
     * failure is assumed.
     */
    public static final int RDC_TIMEOUT_MILLIS = 5000;


    /**
     * Key used to store the preference for the UUID to disk.
     */
    private static final String PREFS_UUID = "UUID";


    /**
     * Boolean tracking whether the service has previously been started.
     */
    private boolean started;

    /**
     * Combined binder object for handling IPC calls.
     */
    private CombinedBinder binder;

    /**
     * The universally unique identity of this middleware instance.
     */
    private Middleware middleware;

    /**
     * Set of endpoints currently active in the middleware.
     */
    private EndpointSet endpointSet;

    /**
     * Pool of open multiplexers within the middleware.
     */
    private MultiplexerPool multiplexerPool;

    /**
     * Switch handling message-based communications at the transport and network layers.
     */
    private MessageSwitch messageSwitch;

    /**
     * Switch handling message-based communications at the transport and network layers.
     */
    private RequestSwitch requestSwitch;

    /**
     * Indicates whether it should be possible for remote instances of the
     * middleware to force commands to run on this instance.
     */
    private boolean forceable;

    /**
     * Stores the location of the remote host on which the resource discovery component
     * (RDC) is accessible.
     */
    private Location rdcLocation;

    /**
     * Indicates whether the middleware instance should be discoverable
     * via the registered RDC.
     */
    private boolean discoverable;

    /**
     * Instance of the middleware database used for persistence.
     */
    private MiddlewareDatabase database;

    /**
     * Indicates that some event occurred since the last update tick invalidating
     * this middleware's RDC entry.
     */
    private boolean shouldUpdateRDC;

    /**
     * Executor for running update tasks with timeout.
     */
    private ExecutorService updateExecutor;


    /**
     * Initialise the service fields and set up the communications switch.
     *
     * @return whether the middleware was started.
     */
    private synchronized boolean start() {
        if (started) {
            return false;
        }

        Log.i(getTag(), "Middleware starting...");
        Toast.makeText(this, getText(R.string.toast_mw_starting), Toast.LENGTH_SHORT).show();

        // Initialise object parameters.
        binder = new CombinedBinder(this);
        endpointSet = new EndpointSet();
        multiplexerPool = new MultiplexerPool(this);

        messageSwitch = new MessageSwitch(
                Arrays.asList(
                        new ZMQSchemeConfiguration(ZMQSchemeConfiguration.DEFAULT_MESSAGE_PORT)
                ));

        ControlMessageHandler handler = new ControlMessageHandler(this);
        requestSwitch = new RequestSwitch(
                Arrays.asList(
                        new ZMQSchemeConfiguration(ZMQSchemeConfiguration.DEFAULT_REQUEST_PORT)
                ),
                handler);

        // Restore the middleware UUID from persistent storage.
        SharedPreferences prefs = getSharedPreferences("preferences", 0);
        long uuid = prefs.getLong(PREFS_UUID, -1);
        if (uuid == -1) {
            uuid = new Random(System.nanoTime()).nextLong() & 0x7FFFFFFFL;
            prefs.edit()
                    .putLong(PREFS_UUID, uuid)
                    .apply();
        }

        middleware = new Middleware(
                uuid,
                messageSwitch.getLocation(),
                requestSwitch.getLocation());

        forceable = true;
        discoverable = true;

        // Restore the previous middleware state from the database.
        database = MiddlewareDatabase.getInstance(this);
        database.restore(this);

        // Set up the RDC update ticker.
        updateExecutor = Executors.newSingleThreadExecutor();
        ScheduledExecutorService updateScheduler = Executors.newSingleThreadScheduledExecutor();
        updateScheduler.scheduleWithFixedDelay(
                this::maybeUpdateRDC,
                RDC_DELAY_MILLIS,
                RDC_DELAY_MILLIS,
                TimeUnit.MILLISECONDS);

        Log.i(getTag(), "Middleware started successfully.");
        started = true;
        return true;
    }

    /**
     * Indicate that an UPDATE control message should be sent to the RDC at the next
     * update tick.
     */
    private synchronized void scheduleUpdateRDC() {
        shouldUpdateRDC = true;
    }

    /**
     * If some previous event marked that we should update our entry in the RDC
     * by calling scheduleUpdateRDC, send an UPDATE control message using the
     * updateRDC routine.
     */
    private void maybeUpdateRDC() {
        // Determine, in a thread-safe manner, whether we should update the RDC.
        boolean update, remove;
        synchronized (this) {
            update = shouldUpdateRDC;
            remove = !discoverable;
        }

        if (update) {
            // Determine whether we should update our entry with the RDC, or
            // remove it, depending on whether we are discoverable.
            Runnable task = (remove) ? this::removeRDC : this::updateRDC;

            // Run the task in the background, allowing a timeout if the
            // RDC fails to respond.
            Future<?> future = updateExecutor.submit(task);

            try {
                // Limit the execution of the task with a timeout.
                future.get(RDC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                Log.i(getTag(), "Update tick completed successfully.");

                // If we were successful, don't update on the next tick (unless
                // another event invalidates our RDC entry).
                synchronized (this) {
                    shouldUpdateRDC = false;
                }
            } catch (InterruptedException | TimeoutException e) {
                Log.w(getTag(), "Timeout during RDC update tick.");
            } catch (ExecutionException e) {
                Log.e(getTag(), "Error during RDC update tick.", e);
            } finally {
                future.cancel(true);
            }
        } else {
            Log.d(getTag(), "Ignoring RDC update tick.");
        }
    }

    /**
     * Send an UPDATE control message to the RDC listing all currently exposed endpoints.
     */
    private void updateRDC() {
        if (rdcLocation == null) {
            Log.w(getTag(), "No RDC location set on update.");
            return;
        }

        try {
            Log.i(getTag(), "Sending update to the RDC.");

            // Construct an UPDATE control message with all endpoint details.
            ArrayList<EndpointDetails> details = new ArrayList<>();
            for (Endpoint endpoint : getEndpointSet()) {
                if (endpoint.isExposed()) {
                    details.add(endpoint.getDetails());
                }
            }
            UpdateControlMessage message = new UpdateControlMessage(middleware, details);

            // Get a request stream to the RDC location.
            RequestStream stream = getRequestStream(rdcLocation);

            // Send the control message over the stream.
            message.getResponse(stream);
        } catch (BadHostException e) {
            Log.w(getTag(), "Error sending update to RDC.", e);
        }
    }

    /**
     * Send a REMOVE control message to the RDC indicating that this middleware instance
     * is no longer discoverable.
     */
    private void removeRDC() {
        if (rdcLocation == null) {
            Log.w(getTag(), "No RDC location set on remove.");
            return;
        }

        try {
            Log.i(getTag(), "Sending removal request to the RDC.");

            // Construct an REMOVE control message.
            RemoveControlMessage message = new RemoveControlMessage(middleware);

            // Get a request stream to the RDC location.
            RequestStream stream = getRequestStream(rdcLocation);

            // Send the control message over the stream.
            message.getResponse(stream);
        } catch (BadHostException e) {
            Log.e(getTag(), "Error sending removal request to RDC.", e);
        }
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
        Log.i(getTag(), "Uncached binder returned in onBind.");

        // Attempt to start the service if not already started.
        start();

        return binder;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, R.string.toast_mw_stopped, Toast.LENGTH_SHORT).show();
    }

    /**
     * Create a new endpoint within the middleware, having the given details and settings.
     * The endpoint will have a freshly generated endpoint identifier.
     */
    public void createEndpoint(EndpointDetails details, boolean exposed, boolean forceable,
                               boolean addToDatabase)
            throws EndpointCollisionException, BadSchemaException {
        Log.d(getTag(), "Creating endpoint from details " + details);

        Endpoint endpoint = new Endpoint(this, details, exposed, forceable);

        // Synchronise on the endpoint set to prevent interleaving endpoint
        // destruction and creation/another destruction.
        //noinspection SynchronizeOnNonFinalField
        synchronized (endpointSet) {
            if (endpointSet.getEndpointByName(details.getName()) != null) {
                // An endpoint of this name already exists!
                Log.w(getTag(), "Endpoint with name \"" + details.getName() +
                        "\" already exists.");
                throw new EndpointCollisionException(details.getName());
            }

            // If we have been told to persist the endpoint, do so now.
            if (addToDatabase) {
                database.insertEndpoint(details, exposed, forceable);
            }

            // Add the endpoint to the set.
            endpointSet.add(endpoint);

            Log.i(getTag(), "Endpoint " + endpoint + " created.");
        }

        scheduleUpdateRDC();
    }

    /**
     * Destroy an existing endpoint and all associated mappings, closing any open
     * links having this endpoint at either end. This frees the endpoint name
     * for use in newly created endpoints; however, the endpoint identifier will
     * never be reused (modulo collisions).
     */
    public void destroyEndpoint(String name, boolean removeFromDatabase)
            throws EndpointNotFoundException {
        // Synchronise on the endpoint set to prevent interleaving endpoint
        // destruction and creation/another destruction.
        //noinspection SynchronizeOnNonFinalField
        synchronized (endpointSet) {
            Endpoint endpoint = endpointSet.getEndpointByName(name);
            if (endpoint == null) {
                // If the returned object is null, there is no endpoint by that name.
                throw new EndpointNotFoundException(name);
            }

            // Perform destruction routines for the endpoint and binder.
            binder.invalidateEndpoint(name);
            endpoint.destroy();
            endpointSet.remove(endpoint);

            // If we have been told to persist the endpoint deletion, do so now.
            if (removeFromDatabase) {
                database.deleteEndpoint(name);
            }

            Log.i(getTag(), "Endpoint with name \"" + name + "\" destroyed.");
        }

        scheduleUpdateRDC();
    }

    /**
     * Return the unique pooled multiplexer for the given remote location.
     *
     * @param remote Remote middleware instance with which the multiplexer communicates.
     *
     * @return a reference to a Multiplexer object.
     *
     * @throws BadHostException if it was impossible to create an underlying message
     *                          stream to the given host.
     */
    public Multiplexer getMultiplexer(Middleware remote) throws BadHostException {
        return multiplexerPool.getMultiplexer(remote);
    }

    /**
     * Return a message stream to the given location, preferring certain
     * network schemes according to the set policy.
     *
     * @param remote Location of the remote host to get a message stream to.
     *
     * @return a reference to a message stream.
     *
     * @throws BadHostException if it was impossible to create a message stream to the
     *                          given host.
     */
    public MessageStream getMessageStream(Location remote) throws BadHostException {
        try {
            return messageSwitch.getStream(remote.priorityAddress());
        } catch (NoValidAddressException e) {
            throw new BadHostException(remote.toString());
        }
    }

    /**
     * Return a request stream to the given location, preferring certain
     * network schemes according to the set policy.
     *
     * @param remote Location of the remote host to get a request stream to.
     *
     * @return a reference to a request stream.
     *
     * @throws BadHostException if it was impossible to create a message stream to the
     *                          given host.
     */
    public RequestStream getRequestStream(Location remote) throws BadHostException {
        try {
            return requestSwitch.getStream(remote.priorityAddress());
        } catch (NoValidAddressException e) {
            throw new BadHostException(remote.toString());
        }
    }

    /**
     * Send a QUERY control message to the registered RDC, returning the resultant
     * list of middleware instances. If no instances match the query, an empty list is
     * returned.
     *
     * @return a list of locations exposing endpoints which match the query.
     *
     * @throws BadHostException if the middleware fails to connect to the RDC location.
     */
    public List<Middleware> discover(Query query) throws BadHostException {
        Log.i(getTag(), "Querying RDC for peers matching " + query);

        // Construct a QUERY control message with the given query.
        QueryControlMessage message = new QueryControlMessage(query);

        // Get a request stream to the RDC location.
        RequestStream stream = getRequestStream(rdcLocation);

        // Send the control message over the stream, and return the response from the RDC.
        QueryControlMessage.Response response = message.getResponse(stream);
        return response.getMiddlewares();
    }

    /**
     * Filter existing endpoints by a query, and open links from all matching
     * to a single given remote endpoint.
     *
     * @param query Query with which to filter local endpoints.
     * @param remote Remote endpoint to which to open links.
     *
     * @return a list of endpoint-details for the opened links.
     */
    public List<RemoteEndpointDetails> openLinks(Query query, RemoteEndpointDetails remote) {
        Log.i(getTag(), "Opening links to " + remote.toLogString() + " on middleware " +
                remote.getMiddleware() + " from local endpoints matching " + query);

        // Keep track of endpoints returned.
        List<RemoteEndpointDetails> endpoints = new ArrayList<>();

        //noinspection SynchronizeOnNonFinalField
        synchronized (endpointSet) {
            // Filter the endpoint set according to the query.
            StreamSupport.stream(endpointSet)
                    .filter(e -> query.getFilter().test(e.getRemoteDetails()))
                    .forEach(e -> {
                        try {
                            e.openLink(remote);
                            endpoints.add(e.getRemoteDetails());
                        } catch (BadHostException | UnexpectedClosureException ex) {
                            Log.w(getTag(), "Error opening link on endpoint " + e, ex);
                        }
                    });

            // Return the filtered link details.
            return endpoints;
        }
    }

    /**
     * Run a general middleware command on a remote instance of the middleware.
     *
     * @param address The location of the device the middleware is accessible on.
     * @param command Object describing the command.
     *
     * @throws BadHostException if the given host is invalid.
     */
    public boolean force(Location address, MiddlewareCommand command) throws BadHostException {
        // Construct a new control message around the command.
        MiddlewareCommandControlMessage message = new MiddlewareCommandControlMessage(command);

        // Open a new request stream to the remote location.
        RequestStream stream = getRequestStream(address);

        // Send the control message over the request stream, and return the result.
        MiddlewareCommandControlMessage.Response response = message.getResponse(stream);
        return response.getSuccess();
    }

    /**
     * Run a command on a remote endpoint (an endpoint of a remote middleware instance).
     *
     * @param address The address of the device the middleware is accessible on.
     * @param name The name of the endpoint to run the command on.
     * @param command Object describing the command.
     *
     * @throws BadHostException if the given host is invalid.
     */
    public boolean forceEndpoint(Location address, String name, EndpointCommand command)
            throws BadHostException {
        // Construct a new control message around the command.
        EndpointCommandControlMessage message = new EndpointCommandControlMessage(name, command);

        // Open a new request stream to the remote location.
        RequestStream stream = getRequestStream(address);

        // Send the control message over the request stream, and return the result.
        EndpointCommandControlMessage.Response response = message.getResponse(stream);
        return response.getSuccess();
    }

    /**
     * Execute the given command on the middleware.
     *
     * @param command Data representation of the command to run.
     *
     * @return whether the command ran successfully.
     */
    public boolean execute(MiddlewareCommand command) {
        Log.d(getTag(), "Received command: \"" + command.toJSON() + "\"");

        if (!forceable) {
            Log.w(getTag(), "Received command ignored (not forceable).");
            return false;
        }

        try {
            if (command instanceof SetRDCAddressCommand) {
                // Cast the command to extract the data.
                SetRDCAddressCommand rdcCommand = (SetRDCAddressCommand) command;

                // Build an RDC location with a single address.
                Location location = new Location();
                location.addAddress(Address.make(rdcCommand.getAddress()));

                // Set the RDC location.
                setRDCLocation(location);
                return true;
            }

            Log.e(getTag(), "Unsupported command received.");
        } catch (MalformedAddressException e) {
            Log.e(getTag(), "Error forcing command on middleware: ", e);
        }

        return false;
    }

    public Middleware getMiddleware() {
        return middleware;
    }

    public EndpointSet getEndpointSet() {
        return endpointSet;
    }

    public boolean isForceable() {
        return forceable;
    }

    public synchronized void setForceable(boolean forceable) {
        Log.i(getTag(), "Middleware forceable flag set " + forceable);
        this.forceable = forceable;
    }

    public synchronized void setRDCLocation(Location location) {
        Log.i(getTag(), "RDC location set to " + location);
        this.rdcLocation = location;
    }

    public synchronized void setDiscoverable(boolean discoverable) {
        if (discoverable == this.discoverable) {
            return;
        }

        Log.i(getTag(), "Middleware discoverable flag set " + discoverable);

        this.discoverable = discoverable;
        scheduleUpdateRDC();
    }

    public MiddlewareDatabase getDatabase() {
        return database;
    }

    private static String getTag() {
        return "MW";
    }
}
