package uk.ac.cam.seh208.middleware.core.network;

import java.util.TreeSet;


/**
 * Identifier for a unique instance of the middleware, accessible on various
 * addresses on different interfaces.
 */
public class Location {

    /**
     * Universally unique identifier of the middleware instance. This is used
     * to identify middleware instances that may be contacted via multiple interfaces.
     */
    private long uuid;

    /**
     * Collection of addresses on which the middleware instance is accessible.
     */
    private TreeSet<Address> addresses;


    /**
     * Instantiate a new location with the given uuid and a blank address set.
     */
    public Location(int uuid) {
        this.uuid = uuid;
        addresses = new TreeSet<>();
    }

    /**
     * Associate a new address with this location.
     *
     * @param address The concrete address.
     *
     * @return whether the address was successfully added.
     */
    public boolean addAddress(Address address) {
        return addresses.add(address);
    }

    /**
     * Disassociate an address with this location.
     *
     * @param address The conrete address (which already exists in the address set).
     *
     * @return whether the address was successfully removed.
     */
    public boolean removeAddress(Address address) {
        return addresses.remove(address);
    }

    /**
     * Return a string encoding of the location.
     *
     * @return a reference to a String object.
     */
    @Override
    public String toString() {
        // Use a builder to prevent unnecessary string copying.
        StringBuilder builder = new StringBuilder();

        // Begin the string with the UUID.
        builder.append(Long.toString(uuid, 16));

        // Append all serialised addresses.
        for (Address address : addresses) {
            builder.append("|");
            builder.append(address);
        }

        // Build the output string.
        return builder.toString();
    }
}
