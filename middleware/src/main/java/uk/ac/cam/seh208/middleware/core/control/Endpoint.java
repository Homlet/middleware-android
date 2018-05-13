package uk.ac.cam.seh208.middleware.core.control;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.LongSparseArray;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java8.util.function.Predicate;
import java8.util.stream.StreamSupport;
import uk.ac.cam.seh208.middleware.common.CloseAllCommand;
import uk.ac.cam.seh208.middleware.common.EndpointCommand;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.IMessageListener;
import uk.ac.cam.seh208.middleware.common.MapCommand;
import uk.ac.cam.seh208.middleware.common.Persistence;
import uk.ac.cam.seh208.middleware.common.Polarity;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.common.UnmapAllCommand;
import uk.ac.cam.seh208.middleware.common.exception.BadHostException;
import uk.ac.cam.seh208.middleware.common.exception.BadQueryException;
import uk.ac.cam.seh208.middleware.common.exception.BadSchemaException;
import uk.ac.cam.seh208.middleware.common.exception.MappingNotFoundException;
import uk.ac.cam.seh208.middleware.common.exception.ProtocolException;
import uk.ac.cam.seh208.middleware.common.exception.SchemaMismatchException;
import uk.ac.cam.seh208.middleware.common.exception.WrongPolarityException;
import uk.ac.cam.seh208.middleware.core.MiddlewareService;
import uk.ac.cam.seh208.middleware.core.exception.UnexpectedClosureException;
import uk.ac.cam.seh208.middleware.core.comms.RequestStream;


/**
 * Object encapsulating the state of an active endpoint within the middleware.
 */
public class Endpoint {

    /**
     * Reference to the containing instance of the middleware service.
     */
    private final MiddlewareService service;

    /**
     * The defining details associated with this endpoint. Once set, these cannot
     * be modified (EndpointDetails objects are immutable), nor can this reference
     * be reset (it is final).
     */
    private final EndpointDetails details;

    /**
     * Indicates whether this endpoint should be exposed to remote instances of the
     * middleware; i.e. whether the middleware should allow remote middleware instances
     * to establish mappings with the bound endpoint if it matches a received query.
     */
    private boolean exposed;

    /**
     * Whether it should be possible for remote instances of the middleware
     * to force commands to run on this endpoint.
     *
     * NOTE: when the forceable field is set as false for the middleware,
     *       this endpoint-specific value is ignored.
     */
    private boolean forceable;

    /**
     * Compiled schema validator for checking that outgoing/incoming messages
     * match the schema.
     */
    private final JsonSchema validator;

    /**
     * Collection of application listeners used to respond to messages.
     */
    private final Set<IMessageListener> listeners;

    /**
     * Map of channels owned by the endpoint; i.e. having the
     * endpoint at their near end, addressed by their unique identifier.
     */
    private final LongSparseArray<Channel> channels;

    /**
     * Map of mappings established from this endpoint, indexed by their unique identifier.
     */
    private final LongSparseArray<Mapping> mappings;

    /**
     * Map of multiplexers carrying channels from this endpoint, indexed by the UUID
     * of their remote location.
     */
    private final LongSparseArray<Multiplexer> multiplexers;


    /**
     * Construct a new endpoint with the given parent service, details and options.
     */
    public Endpoint(MiddlewareService service, EndpointDetails details,
                    boolean exposed, boolean forceable)
            throws BadSchemaException {
        this.service = service;
        this.details = details;
        this.exposed = exposed;
        this.forceable = forceable;

        try {
            // Construct the schema validator object.
            JsonNode schema = JsonLoader.fromString(details.getSchema());
            validator = JsonSchemaFactory.byDefault().getJsonSchema(schema);
        } catch (IOException | ProcessingException e) {
            throw new BadSchemaException(details.getSchema());
        }

        listeners = new HashSet<>();
        channels = new LongSparseArray<>();
        mappings = new LongSparseArray<>();
        multiplexers = new LongSparseArray<>();
    }

    /**
     * Close all active mappings, and all remaining channels.
     */
    public void destroy() {
        unmapAll();
        closeAllChannels();
    }

    /**
     * Send a string message over the multiplexer (provided the endpoint polarity permits
     * this). The message must be JSON formatted and conform to the endpoint message
     * schema; if not, an exception will be thrown.
     *
     * @param message JSON string representation of the message to send.
     *
     * @throws WrongPolarityException when the bound endpoint polarity does not permit sending.
     * @throws SchemaMismatchException when the message string does not match the endpoint schema.
     */
    public void send(String message) throws WrongPolarityException, SchemaMismatchException {
        if (!getPolarity().supportsSending) {
            throw new WrongPolarityException(getPolarity());
        }

        if (!validate(message)) {
            throw new SchemaMismatchException(message, details.getSchema());
        }

        synchronized (this) {
            // Dispatch the message to all multiplexers carrying channels for this endpoint.
            for (int i = 0; i < multiplexers.size(); i++) {
                multiplexers.valueAt(i).send(this, message);
            }
        }
    }

    /**
     * Add a new message listener to the listeners list (provided the endpoint
     * polarity permits this). The listener must implement the IMessageListener interface
     * specified in AIDL.
     *
     * Whenever a new message is received over a JeroMQ mapping socket, it is dispatched
     * to all of the registered listeners via a remote call to their onMessage methods.
     *
     * In the case that the process hosting the listener implementation terminates, the
     * listener will automatically be removed from the endpoint. Therefore, listening
     * processes should re-register listeners when continuing after being killed.
     *
     * @param listener Object implementing the IMessageListener interface, which will be
     *                 remoted by Android allowing the middleware to call its methods.
     */
    public void registerListener(IMessageListener listener) throws RemoteException {
        if (!getPolarity().supportsListeners) {
            throw new WrongPolarityException(getPolarity());
        }

        synchronized (listeners) {
            // On death of its host process, remove the listener from the list.
            IBinder.DeathRecipient recipient = () -> unregisterListener(listener);
            listener.asBinder().linkToDeath(recipient, 0);
            listeners.add(listener);
        }
    }

    /**
     * Remove a message listener from the listeners list. Once unregistered, the
     * listener will no longer be invoked when new messages arrive on the endpoint.
     *
     * @param listener Object previously remoted and registered as a listener.
     */
    public void unregisterListener(IMessageListener listener)  {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Remove all message listeners from the listeners list.
     */
    public void clearListeners() {
        synchronized (listeners) {
            listeners.clear();
        }
    }

    /**
     * Perform an indirect mapping from this endpoint. This consists of two stages:
     *
     *   1. use the registered RDC to discover the locations of peers exposing endpoints
     *      matching the given query;
     *   2. establish channels with one or more endpoints from one or more of these
     *      peers, by sending queries to each in turn.
     *
     * The schema and polarity fields in the given query must be left empty; these are
     * determined by the endpoint schema and polarity.
     *
     * The return value is a list of the endpoints that were successfully mapped to.
     *
     * @param query An endpoint query object for resource discovery and filtering
     *              remote endpoints.
     * @param persistence Persistence level to use for the resultant mapping.
     *
     * @return a list of RemoteEndpointDetails objects representing the endpoints that were
     *         successfully mapped to during the operation.
     *
     * @throws BadQueryException if either of the schema or polarity fields are set in the query.
     * @throws BadHostException when the set RDC host is invalid.
     * @throws ProtocolException if the RDC breaks protocol.
     */
    public Mapping map(Query query, Persistence persistence)
        throws BadQueryException, BadHostException, ProtocolException {
        List<Middleware> remotes = service.discover(query);
        return establishMapping(remotes, query, persistence);
    }

    /**
     * Perform a direct mapping on the bound endpoint. This is similar to the second stage
     * of an indirect mapping: the query is sent to a given peer in order to establish
     * mappings with remote endpoints.
     *
     * The schema and polarity fields in the given query must be left empty; these are
     * determined by the schema and polarity of the bound endpoint.
     *
     * The return value is a list of the endpoints that were successfully mapped to.
     *
     * @param remote Remote middleware instance with which to map.
     * @param query An endpoint query object for filtering remote endpoints.
     * @param persistence Persistence level to use for the resultant mapping.
     *
     * @return a list of RemoteEndpointDetails objects representing the endpoints that were
     *         successfully mapped to during the operation.
     *
     * @throws BadQueryException if either of the schema or polarity fields are set in the query.
     */
    public Mapping mapTo(Middleware remote, Query query, Persistence persistence)
            throws BadQueryException {
        return establishMapping(Collections.singletonList(remote), query, persistence);
    }

    /**
     * Close a mapping associated with this endpoint, referenced by its unique mapping
     * identifier. The process of closing the mapping will close any remaining owned
     * channels.
     *
     * @param mappingId Unique long identifier of the mapping.
     *
     * @throws MappingNotFoundException if the mapping identifier is not recognised
     *                                  for this endpoint.
     */
    public synchronized void unmap(long mappingId) throws MappingNotFoundException {
        if (mappings.indexOfKey(mappingId) < 0) {
            throw new MappingNotFoundException(mappingId);
        }

        // Close the mapping, automatically removing it from the mappings map.
        mappings.get(mappingId).close();
    }

    /**
     * Close all active mappings on this endpoint.
     */
    public synchronized void unmapAll() {
        int size = mappings.size();
        for (int i = 0; i < size; i++) {
            mappings.valueAt(0).close();
        }
    }

    /**
     * Close all channels from this endpoint which match a given query.
     *
     * @param query Query used to filter the channels from this endpoint.
     *
     * @return the number of channels that were closed.
     */
    public synchronized int closeChannels(Query query) {
        // Create a list of channels to close.
        List<Channel> toClose = new ArrayList<>();
        Predicate<EndpointDetails> filter = query.getFilter();

        // Populate the list with channels that match the query.
        for (int i = 0; i < channels.size(); i++) {
            Channel channel = channels.valueAt(i);

            if (filter.test(channel.getRemote())) {
                toClose.add(channel);
            }
        }

        // Close all channels that matched the query.
        StreamSupport.stream(toClose).forEach(Channel::close);

        // Return the count of closed channels.
        return toClose.size();
    }

    /**
     * Close all channels from this endpoint.
     *
     * @return the number of channels that were closed.
     */
    public synchronized int closeAllChannels() {
        int size = channels.size();

        for (int i = 0; i < size; i++) {
            channels.valueAt(0).close();
        }

        return size;
    }

    /**
     * Internal function for establishing a mapping with a number of remote hosts. A given
     * query is sent to each host in turn in order to establish channels, up to a maximum
     * number given in the query.
     *
     * The schema and polarity fields in the given query must be left empty; these are
     * determined by the schema and polarity of the bound endpoint.
     *
     * The return value is a list of the endpoints that were successfully mapped to.
     *
     * @param remotes List of remote middleware instances with which to open channels.
     * @param query An endpoint query object for filtering remote endpoints.
     * @param persistence Persistence level to use for the resultant mapping.
     *
     * @return a list of RemoteEndpointDetails objects representing the endpoints that were
     *         successfully mapped to during the operation.
     *
     * @throws BadQueryException if either of the schema or polarity fields are set in the query.
     */
    private Mapping establishMapping(List<Middleware> remotes, Query query,
                                     Persistence persistence)
        throws BadQueryException {
        // Check that the query is properly formed.
        if (query.schema != null || query.polarity != null) {
            throw new BadQueryException();
        }

        // Set the schema and polarity fields in the query.
        Polarity complement = (details.getPolarity() == Polarity.SOURCE) ? Polarity.SINK
                                                                         : Polarity.SOURCE;
        query = new Query.Builder()
                .copy(query)
                .setSchema(details.getSchema())
                .setPolarity(complement)
                .build();

        synchronized (this) {
            // Keep track of the currently established channels.
            List<Channel> mapChannels = establishChannels(remotes, query);

            // Build the mapping object.
            Mapping mapping = new Mapping(this, query, persistence, mapChannels);

            // No need for subscribeIfOpen here because the mapping must still be open.
            mapping.subscribe(m -> mappings.remove(m.getMappingId()));

            // Put the mapping object in the mappings list before returning it.
            mappings.put(mapping.getMappingId(), mapping);

            return mapping;
        }
    }

    /**
     * Establish some number of channels to a collection of remote instances of the
     * middleware by sending a OPEN-CHANNELS control message, containing the given query.
     *
     * The established channels are opened using the openChannel method, meaning
     * they are automatically added to the channels map. However, they are not
     * automatically associated with a mapping.
     *
     * @param remotes List of remote instances of the middleware to open channels with.
     * @param query Query used to filter the remote endpoints.
     */
    List<Channel> establishChannels(List<Middleware> remotes, Query query) {
        List<Channel> establishedChannels = new ArrayList<>();

        // Establish channels with each host in turn.
        for (Middleware remote : remotes) {
            // If we have accepted the maximum number of channels, we need not
            // contact the remaining remote hosts.
            if (establishedChannels.size() == query.matches) {
                break;
            }

            // Modify the sent query to only accept the remaining number of channels.
            Query modifiedQuery = new Query.Builder()
                    .copy(query)
                    .setMatches(query.matches - establishedChannels.size())
                    .build();

            // Establish channels to endpoints on the remote host, and add them to our list.
            try {
                establishedChannels.addAll(establishChannels(remote, modifiedQuery));
            } catch (BadHostException e) {
                Log.w(getTag(), "Unable to establish channels to host (" +
                        remote.toJSON() + ").");
            }
        }

        return establishedChannels;
    }

    /**
     * Establish some number of channels to a remote instance of the middleware by
     * sending a OPEN-CHANNELS control message, containing the given query.
     *
     * The established channels are opened using the openChannel method, meaning
     * they are automatically added to the channels map. However, they are not
     * automatically associated with a mapping.
     *
     * @param remote Remote instance of the middleware to open channels with.
     * @param query Query used to filter the remote endpoints.
     */
    private List<Channel> establishChannels(Middleware remote, Query query)
            throws BadHostException {
        // Send an OPEN-CHANNELS control message to the remote host.
        OpenChannelsControlMessage message =
                new OpenChannelsControlMessage(getRemoteDetails(), query);
        RequestStream stream = service.getRequestStream(remote.getRequestLocation());
        OpenChannelsControlMessage.Response response = message.getResponse(stream);

        // The response to the OPEN-CHANNELS message contains a list of remote
        // endpoint-details from which channels were opened. Open local
        // counterparts to these channels and track them in the returned list.
        List<Channel> establishedChannels = new ArrayList<>();
        for (RemoteEndpointDetails endpoint : response.getDetails()) {
            try {
                establishedChannels.add(openChannel(endpoint));
            } catch (UnexpectedClosureException e) {
                // Do nothing; whilst the remote currently believes this channel to be
                // open, any attempt to communicate over it will either lead to a
                // ConnectionFailedException or a CLOSE-CHANNEL control response.
                Log.w(getTag(), "Couldn't establish channel to remote endpoint due to " +
                        "unexpected closure (" + endpoint.getEndpointId() + ")");
            }
        }

        return establishedChannels;
    }

    /**
     * Open a channel to a remote endpoint. This affects only the local state, and
     * assumes the remote endpoint will be/has been informed that this channel exists.
     *
     * @param remote Remote endpoint to which the channel should be opened.
     *
     * @return the newly generated id of the channel.
     */
    public Channel openChannel(RemoteEndpointDetails remote)
            throws BadHostException, UnexpectedClosureException {
        Log.i(getTag(), "Opening channel to endpoint " + remote.toLogString() +
                " on middleware [" + remote.getMiddleware().getUUID() + "]");

        // Create a new channel from this endpoint to the remote endpoint.
        Channel channel = new Channel(this, remote);

        synchronized (this) {
            // Get the multiplexer to the remote location.
            long uuid = remote.getMiddleware().getUUID();
            Multiplexer multiplexer = service.getMultiplexer(remote.getMiddleware());
            multiplexers.put(uuid, multiplexer);

            if (!multiplexer.subscribeIfOpen(m -> multiplexers.remove(uuid))) {
                // Remove this multiplexer.
                multiplexers.remove(uuid);

                throw new UnexpectedClosureException("Unexpected multiplexer closure " +
                        "whilst opening channel.");
            }

            // Carry the channel on the multiplexer.
            if (!multiplexer.carryChannel(channel)) {
                throw new UnexpectedClosureException("Unexpected multiplexer closure " +
                        "whilst opening channel.");
            }

            // Track the open channel in the channel map.
            channels.put(channel.getChannelId(), channel);

            // Subscribe to channel closure, by removing it from the channel map.
            if (!channel.subscribeIfOpen(c -> channels.remove(c.getChannelId()))) {
                channels.remove(channel.getChannelId());
                throw new UnexpectedClosureException("Unexpected channel closure " +
                        "whilst opening.");
            }
        }

        return channel;
    }

    /**
     * Callback to be registered as a message handler with multiplexers.
     *
     * Distributes newly received messages to all registered listeners,
     * de-multiplexed by channel identifier.
     *
     * @param channelId The identifier of the channel on which the message
     *                  was received.
     * @param message The newly received message string.
     */
    void onMessage(long channelId, String message) {
        if (channels.indexOfKey(channelId) < 0) {
            // If the channel identifier is not in the channel set, this
            // message shouldn't have ended up here.
            Log.e(getTag(), "Received message from unknown channel ID (" +
                    channelId + ")");
            return;
        }

        if (!validate(message)) {
            // The message does not match the schema; the remote endpoint has broken
            // protocol, and the channel must be closed.
            Log.e(getTag(), "Incoming message schema mismatch on channel (" +
                    channelId + ")");

            channels.get(channelId).close();
            return;
        }

        // Dispatch the message to each of the listeners' onMessage methods
        // in turn, logging the case where a remote error occurs.
        int failures = 0;
        synchronized (listeners) {
            for (IMessageListener listener : listeners) {
                try {
                    listener.onMessage(message);
                } catch (RemoteException e) {
                    failures++;
                }
            }
        }
        if (failures > 0) {
            Log.e(getTag(), "Error occurred dispatching message to " +
                    failures + " listener(s).");
        }
    }

    /**
     * Execute the given command on the endpoint.
     *
     * @param command Data representation of the command to run.
     *
     * @return whether the command ran successfully.
     */
    boolean execute(EndpointCommand command) {
        Log.d(getTag(), "Received command: \"" + command.toJSON() + "\"");

        if (!forceable || !service.isForceable()) {
            Log.w(getTag(), "Received command ignored (not forceable).");
            return false;
        }

        try {
            if (command instanceof MapCommand) {
                // Cast the command to extract the data.
                MapCommand mapCommand = (MapCommand) command;

                // Run the map command.
                map(mapCommand.getQuery(), mapCommand.getPersistence());
                return true;
            }

            if (command instanceof UnmapAllCommand) {
                // Run the unmap command.
                unmapAll();
                return true;
            }

            if (command instanceof CloseAllCommand) {
                // Run the close all channels command.
                closeAllChannels();
                return true;
            }

            Log.e(getTag(), "Unsupported command received.");
        } catch (BadQueryException | BadHostException | ProtocolException e) {
            Log.e(getTag(), "Error forcing command on endpoint: ", e);
        }

        return false;
    }

    /**
     * Convenience method for getting the endpoint identifier from the details.
     *
     * @return the endpoint identifier.
     */
    @SuppressWarnings("WeakerAccess")
    public long getEndpointId() {
        return details.getEndpointId();
    }

    /**
     * Convenience method for getting the endpoint name from the details.
     *
     * @return the endpoint name.
     */
    public String getName() {
        return details.getName();
    }

    /**
     * Convenience method for getting the endpoint polarity from the details.
     *
     * @return the endpoint polarity.
     */
    Polarity getPolarity() {
        return details.getPolarity();
    }

    public EndpointDetails getDetails() {
        return details;
    }

    /**
     * @return a newly constructed RemoteEndpointDetails object referencing
     */
    public RemoteEndpointDetails getRemoteDetails() {
        return new RemoteEndpointDetails(details, service.getMiddleware());
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    public void setForceable(boolean forceable) {
        this.forceable = forceable;
    }

    MiddlewareService getService() {
        return service;
    }

    /**
     * Validate a string message against the endpoint schema.
     *
     * @param message Message to test against the schema.
     *
     * @return whether the message matches the endpoint schema.
     */
    private boolean validate(String message) {
        try {
            // Parse the message as JSON and attempt to validate it against the schema.
            JsonNode parsedMessage = JsonLoader.fromString(message);
            ProcessingReport report = validator.validate(parsedMessage);
            return report.isSuccess();
        } catch (IOException | ProcessingException e) {
            return false;
        }
    }

    /**
     * Get the Android logcat tag for this endpoint.
     */
    private String getTag() {
        return "ENDPOINT" + this;
    }

    @Override
    public String toString() {
        return details.toLogString();
    }
}
