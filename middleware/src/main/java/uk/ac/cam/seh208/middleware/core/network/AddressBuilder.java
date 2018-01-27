package uk.ac.cam.seh208.middleware.core.network;


/**
 * Interface for builders of addresses that can be serialised to strings.
 */
public abstract class AddressBuilder {

    /**
     * Build the address from a string in the associated address language.
     *
     * @param string The address string.
     *
     * @return a reference to this.
     */
    public abstract AddressBuilder fromString(String string) throws MalformedAddressException;

    /**
     * Return the immutable address.
     *
     * @return reference to a newly constructed Address object.
     */
    public abstract Address build();
}
