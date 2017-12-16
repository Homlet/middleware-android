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

import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.IMessageListener;
import uk.ac.cam.seh208.middleware.common.Polarity;
import uk.ac.cam.seh208.middleware.common.exception.BadSchemaException;
import uk.ac.cam.seh208.middleware.common.exception.ListenerNotFoundException;
import uk.ac.cam.seh208.middleware.common.exception.SchemaMismatchException;
import uk.ac.cam.seh208.middleware.common.exception.WrongPolarityException;


/**
 * Object encapsulating the state of an active endpoint within the middleware.
 */
public class Endpoint {
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
    private ArrayList<IMessageListener> listeners;


    public Endpoint(EndpointDetails details, boolean exposed, boolean forceable)
            throws BadSchemaException {
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
    }

    public Endpoint(EndpointDetails details) throws BadSchemaException {
        this(details, true, true);
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

        // TODO: forward message along JeroMQ socket(s).
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
     * Callback to be registered as a message handler with JeroMQ mapping
     * sockets. This will be called whenever a message is received over any
     * remote endpoint mapping to this endpoint.
     *
     * @param message The newly received message string.
     */
    private synchronized void onMessage(String message /* TODO: include mapping. */) {
        if (!validate(message)) {
            // The message does not match the schema; the remote endpoint has broken
            // protocol, and the mapping must be torn down.
            Log.e(getTag(), "Incoming message schema mismatch.");

            // TODO: tear down the mapping.
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
