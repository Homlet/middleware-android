package uk.ac.cam.seh208.middleware.core.comms;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.ArraySet;
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

import java8.util.stream.StreamSupport;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.IMessageListener;
import uk.ac.cam.seh208.middleware.common.Persistence;
import uk.ac.cam.seh208.middleware.common.Polarity;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.common.RemoteEndpointDetails;
import uk.ac.cam.seh208.middleware.common.exception.BadHostException;
import uk.ac.cam.seh208.middleware.common.exception.BadQueryException;
import uk.ac.cam.seh208.middleware.common.exception.BadSchemaException;
import uk.ac.cam.seh208.middleware.common.exception.ListenerNotFoundException;
import uk.ac.cam.seh208.middleware.common.exception.ProtocolException;
import uk.ac.cam.seh208.middleware.common.exception.SchemaMismatchException;
import uk.ac.cam.seh208.middleware.common.exception.WrongPolarityException;
import uk.ac.cam.seh208.middleware.core.MiddlewareService;
import uk.ac.cam.seh208.middleware.core.exception.UnexpectedClosureException;
import uk.ac.cam.seh208.middleware.core.network.Location;
import uk.ac.cam.seh208.middleware.core.network.RequestStream;


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
    private JsonSchema validator;

    /**
     * Collection of application listeners used to respond to messages.
     */
    private Set<IMessageListener> listeners;

    /**
     * Map of channels owned by the endpoint; i.e. having the
     * endpoint at their near end, addressed by their unique identifier.
     */
    private LongSparseArray<Channel> channels;

    /**
     * Collection of mappings established from this endpoint.
     */
    private Set<Mapping> mappings;

    /**
     * Map of multiplexers carrying channels from this endpoint, indexed by the UUID
     * of their remote location.
     */
    private LongSparseArray<Multiplexer> multiplexers;


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
        mappings = new HashSet<>();
        multiplexers = new LongSparseArray<>();
    }

    public Endpoint(MiddlewareService service, EndpointDetails details) throws BadSchemaException {
        this(service, details, true, true);
    }

    /**
     * Initialisation routine run after the endpoint has been integrated into the middleware
     * instance (i.e. after we have ensured there was no name or uuid collision).
     */
    public synchronized void initialise() {
        // TODO: work out what needs to be done (if anything), and implement.
    }

    /**
     * Close all active mappings, and all remaining channels.
     */
    public synchronized void destroy() {
        StreamSupport.stream(mappings).forEach(Mapping::close);
        int size = channels.size();
        for (int i = 0; i < size; i++) {
            channels.valueAt(0).close();
        }
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
    public void send(String message) throws WrongPolarityException, SchemaMismatchException,
                                            IOException, ProcessingException {
        if (!getPolarity().supportsSending) {
            throw new WrongPolarityException(getPolarity());
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

        synchronized (this) {
            // On death of its host process, remove the listener from the list (in a
            // synchronised manner so as not to interfere with existing iterators).
            IBinder.DeathRecipient recipient = () -> {
                synchronized (this) {
                    listeners.remove(listener);
                }
            };
            listener.asBinder().linkToDeath(recipient, 0);
            listeners.add(listener);
        }
    }

    /**
     * Remove a message listener from the listeners list. Once unregistered, the
     * listener will no longer be invoked when new messages arrive on the endpoint.
     *
     * @param listener Object previously remoted and registered as a listener.
     *
     * @throws ListenerNotFoundException when the passed listener is not currently
     *                                   registered with the endpoint.
     */
    public synchronized void unregisterListener(IMessageListener listener)
            throws ListenerNotFoundException {
        if (!listeners.remove(listener)) {
            throw new ListenerNotFoundException();
        }
    }

    /**
     * Remove all message listeners from the listeners list.
     */
    public synchronized void clearListeners() {
        listeners.clear();
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
        List<Location> hosts = service.discover(query);
        return establishMapping(hosts, query, persistence);
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
     * @param host Location on which the peer is accessible.
     * @param query An endpoint query object for filtering remote endpoints.
     * @param persistence Persistence level to use for the resultant mapping.
     *
     * @return a list of RemoteEndpointDetails objects representing the endpoints that were
     *         successfully mapped to during the operation.
     *
     * @throws BadQueryException if either of the schema or polarity fields are set in the query.
     */
    public Mapping mapTo(Location host, Query query, Persistence persistence)
            throws BadQueryException {
        return establishMapping(Collections.singletonList(host), query, persistence);
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
     * @param hosts List of hostnames on which remote middleware instances reside.
     * @param query An endpoint query object for filtering remote endpoints.
     * @param persistence Persistence level to use for the resultant mapping.
     *
     * @return a list of RemoteEndpointDetails objects representing the endpoints that were
     *         successfully mapped to during the operation.
     *
     * @throws BadQueryException if either of the schema or polarity fields are set in the query.
     */
    private Mapping establishMapping(List<Location> hosts, Query query, Persistence persistence)
        throws BadQueryException {
        // Check that the query is properly formed.
        if (query.schema != null || query.polarity != null) {
            throw new BadQueryException();
        }

        // Set the schema and polarity fields in the query.
        // TODO: more elegant solution to finding polarity complement.
        Polarity complement = (details.getPolarity() == Polarity.SOURCE) ? Polarity.SINK
                                                                         : Polarity.SOURCE;
        query = new Query.Builder()
                .copy(query)
                .setSchema(details.getSchema())
                .setPolarity(complement)
                .build();

        // Keep track of the currently established channels.
        List<Channel> mapChannels = new ArrayList<>();

        synchronized (this) {
            // Establish channels with each host in turn.
            for (Location remote : hosts) {
                // If we have accepted the maximum number of channels, we need not
                // contact the remaining remote hosts.
                if (mapChannels.size() == query.matches) {
                    break;
                }

                // Modify the sent query to only accept the remaining number of channels.
                Query modifiedQuery = new Query.Builder()
                        .copy(query)
                        .setMatches(query.matches - mapChannels.size())
                        .build();

                // Establish channels to endpoints on the remote host, and add them to our list.
                try {
                    mapChannels.addAll(establishChannels(remote, modifiedQuery));
                } catch (BadHostException e) {
                    Log.w(getTag(), "Unable to establish channels to host (" + remote + ").");
                }
            }

            // Build the mapping object, keeping internal record of it before returning.
            Mapping mapping = new Mapping(this, query, persistence, mapChannels);
            mappings.add(mapping);

            // No need for subscribeIfOpen here because the mapping must still be open.
            mapping.subscribe(mappings::remove);
            return mapping;
        }
    }

    /**
     * Establish some number of channels to a remote instance of the middleware by
     * sending a OPEN-CHANNELS control message, containing the given query.
     *
     * The established channels are opened using the openChannel method, meaning
     * they are automatically added to the channels map. However, they are not
     * automatically associated with a mapping.
     *
     * @param host Location on which the remote instance of the middleware resides.
     * @param query Query used to filter the remote endpoints.
     */
    private List<Channel> establishChannels(Location host, Query query) throws BadHostException {
        // Send an OPEN-CHANNELS control message to the remote host.
        OpenChannelsControlMessage message =
                new OpenChannelsControlMessage(getRemoteDetails(), query);
        RequestStream stream = service.getRequestStream(host);
        OpenChannelsControlMessage.Response response = message.getResponse(stream);

        // Synchronise to prevent messages being sent to an incomplete mapping.
        synchronized (this) {
            // The response to the OPEN-CHANNELS message contains a list of remote
            // endpoint-details from which channels were opened. Open local
            // counterparts to these channels and track them in the returned list.
            List<Channel> establishedChannels = new ArrayList<>();
            for (RemoteEndpointDetails remote : response.getDetails()) {
                try {
                    establishedChannels.add(openChannel(remote));
                } catch (UnexpectedClosureException e) {
                    // Do nothing; whilst the remote currently believes this channel to be
                    // open, any attempt to communicate over it will either lead to a
                    // ConnectionFailedException or a CLOSE-CHANNEL control response.
                    Log.w(getTag(), "Couldn't establish channel to remote endpoint due to " +
                            "unexpected closure (" + remote.getEndpointId() + ")");
                }
            }

            return establishedChannels;
        }
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
        // Create a new channel from this endpoint to the remote endpoint.
        Channel channel = new Channel(this, remote);

        synchronized (this) {
            // Get the multiplexer to the remote location.
            long uuid = remote.getLocation().getUUID();
            Multiplexer multiplexer = service.getMultiplexer(remote.getLocation());
            multiplexers.put(uuid, multiplexer);

            if (multiplexer.subscribeIfOpen(m -> multiplexers.remove(uuid))) {
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
    public synchronized void onMessage(long channelId, String message) {
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

        Log.v(getTag(), "Received new message: \"" + message + "\"");

        // Dispatch the message to each of the listeners' onMessage methods
        // in turn, logging the case where a remote error occurs.
        int failures = 0;
        for (IMessageListener listener : listeners) {
            try {
                listener.onMessage(message);
            } catch (RemoteException e) {
                failures++;
            }
        }
        if (failures > 0) {
            Log.e(getTag(), "Error occurred dispatching message to " +
                    failures + " listener(s).");
        }
    }

    /**
     * Convenience method for getting the endpoint identifier from the details.
     *
     * @return the endpoint identifier.
     */
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
    public Polarity getPolarity() {
        return details.getPolarity();
    }

    public EndpointDetails getDetails() {
        return details;
    }

    /**
     * @return a newly constructed RemoteEndpointDetails object referencing
     */
    public RemoteEndpointDetails getRemoteDetails() {
        return new RemoteEndpointDetails(details, service.getLocation());
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    public boolean isForceable() {
        return forceable;
    }

    public void setForceable(boolean forceable) {
        this.forceable = forceable;
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
        return "ENDPOINT:" + getName();
    }
}
