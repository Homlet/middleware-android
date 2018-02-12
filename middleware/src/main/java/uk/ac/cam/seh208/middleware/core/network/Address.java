package uk.ac.cam.seh208.middleware.core.network;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Objects;

import uk.ac.cam.seh208.middleware.common.JSONSerializable;
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
     * Return a reference to a string representing the address in its
     * canonical form. For instance, an IPv6 implementation would
     * return "::" for addresses loaded as "0::" or "0:0:0:0::" etc.
     *
     * @return a reference to a String object.
     */
    public abstract String toCanonicalString();

    /**
     * Use the canonical string function for translating to string,
     * in order to force an implementation from the subclass.
     *
     * @return a reference to a String object.
     */
    @Override
    public final String toString() {
        return toCanonicalString();
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
