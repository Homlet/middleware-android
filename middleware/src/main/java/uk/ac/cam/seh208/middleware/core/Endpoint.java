package uk.ac.cam.seh208.middleware.core;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.IMessageListener;
import uk.ac.cam.seh208.middleware.common.Persistence;
import uk.ac.cam.seh208.middleware.common.Polarity;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.common.exception.BadHostException;
import uk.ac.cam.seh208.middleware.common.exception.BadQueryException;
import uk.ac.cam.seh208.middleware.common.exception.BadSchemaException;
import uk.ac.cam.seh208.middleware.common.exception.IncompleteBuildException;
import uk.ac.cam.seh208.middleware.common.exception.ListenerNotFoundException;
import uk.ac.cam.seh208.middleware.common.exception.ProtocolException;
import uk.ac.cam.seh208.middleware.common.exception.SchemaMismatchException;
import uk.ac.cam.seh208.middleware.common.exception.WrongPolarityException;


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
     * Collection of listeners used to respond to messages.
     */
    private List<IMessageListener> listeners;

    /**
     * Collection of mappings established from this endpoint.
     */
    private List<Mapping> mappings;


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

        listeners = new ArrayList<>();
        mappings = new ArrayList<>();
    }

    public Endpoint(MiddlewareService service, EndpointDetails details) throws BadSchemaException {
        this(service, details, true, true);
    }

    /**
     * TODO: document.
     */
    public void initialise() {
        // TODO: implement.
    }

    /**
     * TODO: document.
     */
    public void destroy() {
        // TODO: implement.
    }

    /**
     * Send a JSON message over the JeroMQ mapping sockets (provided the endpoint polarity
     * permits this). The message must conform to the endpoint message schema; if not, an
     * exception will be thrown.
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

        // TODO: forward message along channels.
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
    public synchronized void registerListener(IMessageListener listener) throws RemoteException {
        if (!getPolarity().supportsListeners) {
            throw new WrongPolarityException(getPolarity());
        }

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
    public synchronized Mapping map(Query query, Persistence persistence)
        throws BadQueryException, BadHostException, ProtocolException {
        List<String> hosts = service.discover(query);
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
     * @param host Hostname or address of the peer.
     * @param query An endpoint query object for filtering remote endpoints.
     * @param persistence Persistence level to use for the resultant mapping.
     *
     * @return a list of RemoteEndpointDetails objects representing the endpoints that were
     *         successfully mapped to during the operation.
     *
     * @throws BadQueryException if either of the schema or polarity fields are set in the query.
     */
    public synchronized Mapping mapTo(String host, Query query, Persistence persistence)
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
    private Mapping establishMapping(List<String> hosts, Query query, Persistence persistence)
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
        List<Channel> channels = new ArrayList<>();

        // Establish channels with each host in turn.
        for (String host : hosts) {
            // If we have accepted the maximum number of channels, we need not
            // contact the remaining remote hosts.
            if (channels.size() == query.matches) {
                break;
            }

            // Modify the sent query to only accept the remaining number of channels.
            Query modifiedQuery = new Query.Builder()
                    .copy(query)
                    .setMatches(query.matches - channels.size())
                    .build();

            // Establish channels to endpoints on the remote host, and add them to our list.
            channels.addAll(establishChannels(host, modifiedQuery));
        }

        try {
            // Build the mapping object, keeping internal record of it before returning.
            Mapping mapping = new Mapping.Builder()
                    .setQuery(query)
                    .setPersistence(persistence)
                    .addChannels(channels)
                    .build();
            mappings.add(mapping);
            return mapping;
        } catch (IncompleteBuildException ignored) {
            // This should not be reachable.
            return null;
        }
    }

    /**
     * TODO: document (pull some stuff out of the map and mapTo documentation).
     */
    private List<Channel> establishChannels(String host, Query query) {
        return null;
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
     * Callback to be registered as a message handler with channels.
     *
     * Distributes newly received messages to all registered listeners.
     *
     * @param message The newly received message string.
     */
    public synchronized void onMessage(String message, Channel channel) {
        if (!validate(message)) {
            // The message does not match the schema; the remote endpoint has broken
            // protocol, and the channel must be closed.
            Log.e(getTag(), "Incoming message schema mismatch.");

            channel.close();
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
     * Get the Android logcat tag for this endpoint.
     */
    private String getTag() {
        return "ENDPOINT:" + getName();
    }
}
