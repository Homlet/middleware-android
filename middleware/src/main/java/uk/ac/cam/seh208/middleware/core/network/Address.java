package uk.ac.cam.seh208.middleware.core.network;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.cam.seh208.middleware.common.JSONSerializable;
import uk.ac.cam.seh208.middleware.core.exception.MalformedAddressException;
import uk.ac.cam.seh208.middleware.core.network.impl.ZMQAddress;


/**
 * Interface for addresses that can be serialized to strings.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "tag"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ZMQAddress.class, name = "a0")
})
public abstract class Address implements JSONSerializable {

    /**
     * Scheme string for zmq addresses.
     */
    public static final String SCHEME_ZMQ = "zmq";

    /**
     * Priority used for determining which interface addresses should be
     * preferred for connections when multiple are available.
     */
    private int priority;


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
    public static Address make(String string) throws MalformedAddressException {
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
     * Get the scheme string associated with a given address, determined by its dynamic type.
     *
     * @param address Address to introspect for scheme type.
     *
     * @return a scheme string.
     */
    static String getSchemeString(Address address) {
        if (address instanceof ZMQAddress) {
            return SCHEME_ZMQ;
        }

        // Unreachable.
        return null;
    }

    protected Address(int priority) {
        this.priority = priority;
    }

    int getPriority() {
        return priority;
    }

    /**
     * Return a reference to a string representing the address in its
     * canonical form, preceded by the scheme string in the fully
     * unique format (scheme://address)
     */
    public final String toCanonicalString() {
        return getSchemeString(this) + "://" + toAddressString();
    }

    /**
     * Return a reference to a string representing the address in a
     * form reversible if the scheme is known in advance. For instance,
     * an IPv6 implementation would return "::" for addresses loaded as
     * "0::" or "0:0:0:0::" etc.
     *
     * @return a reference to a String object.
     */
    protected abstract String toAddressString();

    /**
     * Use the canonical string function for translating to string,
     * in order to force an implementation from the subclass.
     *
     * @return a reference to a String object.
     */
    @Override
    public final String toString() {
        return toAddressString();
    }

    /**
     * Compare addresses by comparing canonical strings, checking also that
     * the dynamic type matches.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!getClass().isInstance(obj)) {
            // Canonical address strings are only unique within subclasses
            // of address; only compare them if the other object has the
            // same dynamic type as this object.
            return false;
        }
        Address other = (Address) obj;
        return Objects.equals(toCanonicalString(), other.toCanonicalString());
    }
}
