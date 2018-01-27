package uk.ac.cam.seh208.middleware.core.network;


/**
 * Interface for addresses that can be serialized to strings.
 */
public abstract class Address {

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
     * in order to force an implementation from the user.
     *
     * @return a reference to a String object.
     */
    @Override
    public final String toString() {
        return toCanonicalString();
    }
}
