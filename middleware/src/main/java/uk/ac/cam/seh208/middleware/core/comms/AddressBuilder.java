package uk.ac.cam.seh208.middleware.core.comms;

import uk.ac.cam.seh208.middleware.core.exception.MalformedAddressException;


/**
 * Interface for builders of addresses that can be serialised to strings.
 */
public interface AddressBuilder {

    /**
     * Build the address from a string in the associated address language.
     *
     * @param string The address string.
     *
     * @return a reference to this.
     */
    AddressBuilder fromString(String string) throws MalformedAddressException;

    /**
     * Return the immutable address.
     *
     * @return reference to a newly constructed Address object.
     */
    Address build();
}
