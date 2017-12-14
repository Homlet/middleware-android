package uk.ac.cam.seh208.middleware.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonSchemaFactoryBuilder;

import java.io.IOException;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.Polarity;
import uk.ac.cam.seh208.middleware.common.exception.InvalidSchemaException;
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


    public Endpoint(EndpointDetails details, boolean exposed, boolean forceable)
            throws InvalidSchemaException {
        this.details = details;
        this.exposed = exposed;
        this.forceable = forceable;

        try {
            // Construct the schema validator object.
            JsonNode schema = JsonLoader.fromString(details.getSchema());
            validator = JsonSchemaFactory.byDefault().getJsonSchema(schema);
        } catch (IOException | ProcessingException e) {
            throw new InvalidSchemaException(details.getSchema());
        }
    }

    public Endpoint(EndpointDetails details) throws InvalidSchemaException {
        this(details, true, true);
    }

    public void initialise() {
        // TODO: implement.
    }

    public void destroy() {
        // TODO: implement.
    }

    public void send(String message) throws WrongPolarityException, SchemaMismatchException,
                                            IOException, ProcessingException {
        if (details.getPolarity() != Polarity.SOURCE) {
            // If the endpoint is not a data source, we cannot send messages from it.
            throw new WrongPolarityException(details.getPolarity());
        }

        try {
            // Parse the message as JSON and attempt to validate it against the schema.
            JsonNode parsedMessage = JsonLoader.fromString(message);
            ProcessingReport report = validator.validate(parsedMessage);
            if (!report.isSuccess()) {
                // The message is not valid with respect to the schema; it cannot be sent.
                throw new SchemaMismatchException();
            }
        } catch (IOException | ProcessingException e) {
            // TODO: come up with a new kind of exception for these cases.
            throw e;
        }

        // TODO: forward message along JeroMQ socket.
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
}
