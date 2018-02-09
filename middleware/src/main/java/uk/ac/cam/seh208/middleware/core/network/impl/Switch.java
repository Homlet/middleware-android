package uk.ac.cam.seh208.middleware.core.network.impl;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.cam.seh208.middleware.core.exception.MalformedAddressException;
import uk.ac.cam.seh208.middleware.core.network.Address;
import uk.ac.cam.seh208.middleware.core.network.AddressBuilder;
import uk.ac.cam.seh208.middleware.core.network.MessageContext;
import uk.ac.cam.seh208.middleware.core.network.RequestContext;


/**
 * Switchboard class aggregating many network implementations and switching between
 * them based on address type. Also capable of differentiating between types of
 * address string and constructing new address objects.
 */
public class Switch {

    /**
     * Scheme string for zmq addresses.
     */
    public static final String SCHEME_ZMQ = "zmq";


    /**
     * Build a new address object from the given string in scheme://address
     * form. The subtype of address constructed is determined by the scheme,
     * and then the builder of that type is deferred to for interpretation
     * of the address string.
     *
     * @param string Input scheme://address string.
     *
     * @return a newly constructed object of an Address subtype.
     *
     * @throws MalformedAddressException if the string does not have the form scheme://address
     * @throws MalformedAddressException if the scheme is not supported.
     * @throws MalformedAddressException if the builder cannot parse the address string.
     */
    public static Address makeAddress(String string) throws MalformedAddressException {
        AddressBuilder builder;
        Pattern pattern = Pattern.compile("(.*)://(.*)");
        Matcher matcher = pattern.matcher(string);

        if (!matcher.matches()) {
            // The input string is not of the form scheme://address.
            throw new MalformedAddressException(string);
        }

        switch (matcher.group(1).toLowerCase()) {
            case SCHEME_ZMQ:
                builder = new ZMQAddress.Builder();
                break;

            default:
                // The given scheme is not yet supported by the middleware.
                throw new MalformedAddressException(string);
        }

        return builder.fromString(matcher.group(2)).build();
    }


    /**
     * Store of message contexts by scheme string.
     */
    private HashMap<String, MessageContext> messageContextsByScheme;

    /**
     * Store of request contexts by scheme string.
     */
    private HashMap<String, RequestContext> requestContextsByScheme;


    /**
     * Construct and initialise message and request contexts for the given schemes.
     */
    public Switch(List<String> schemes) {
        messageContextsByScheme = new HashMap<>();
        requestContextsByScheme = new HashMap<>();

        // Create contexts for the requested schemes.
        for (String scheme : schemes) {
            switch (scheme) {
                case SCHEME_ZMQ:
                    messageContextsByScheme.put(scheme, new ZMQMessageContext());
                    requestContextsByScheme.put(scheme, new ZMQRequestContext());
                    break;

                // case SCHEME_BLUETOOTH etc...

                default:
                    // TODO: logging without Android dependencies.
                    break;
            }
        }
    }

    /**
     * Return the message context associated with the given scheme, or null
     * if such a context is not owned by the switch.
     */
    public MessageContext getMessageContext(String scheme) {
        return messageContextsByScheme.get(scheme);
    }

    /**
     * Return the request context associated with the given scheme, or null
     * if such a context is not owned by the switch.
     */
    public RequestContext getRequestContext(String scheme) {
        return requestContextsByScheme.get(scheme);
    }
}
