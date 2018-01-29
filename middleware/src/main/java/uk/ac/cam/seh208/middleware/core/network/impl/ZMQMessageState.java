package uk.ac.cam.seh208.middleware.core.network.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * Stores state associated with the Harmony context.
 */
public class ZMQMessageState {

    /**
     * Map of all currently maintained stream addresses indexed by their unique
     * identity on the ROUTER socket.
     */
    private HashMap<String, ZMQAddress> addressesByIdentity;

    /**
     * Map of all currently maintained streams indexed by their ZeroMQ address.
     */
    private HashMap<String, ZMQMessageStream> streamsByAddress;

    /**
     * ZMQ address on which the ROUTER socket is bound.
     *
     * TODO: reference full location instead.
     */
    private ZMQAddress localAddress;


    /**
     * Instantiate the address and stream maps.
     */
    public ZMQMessageState(ZMQAddress localAddress) {
        addressesByIdentity = new HashMap<>();
        streamsByAddress = new HashMap<>();
        this.localAddress = localAddress;
    }

    /**
     * Insert a new message stream into the state. If the stream already exists in the
     * state, return false.
     *
     * @param address ZeroMQ address of the stream to insert.
     * @param stream Reference to the stream to insert.
     *
     * @return whether the message stream was inserted.
     */
    public synchronized boolean insertStream(ZMQAddress address, ZMQMessageStream stream) {
        String addressString = address.toCanonicalString();

        if (streamsByAddress.containsKey(addressString)) {
            // The stream is present in the address map already.
            return false;
        }

        // Put the stream in the address map.
        streamsByAddress.put(addressString, stream);

        return true;
    }

    /**
     * Atomically insert a stream for an address, and track a given identity against the address.
     *
     * @param address ZeroMQ address of the stream to insert.
     * @param identity ROUTER identity of the address.
     * @param stream Reference to the stream to insert.
     *
     * @return whether the state was updated.
     */
    public synchronized boolean insertStream(ZMQAddress address, String identity,
                                             ZMQMessageStream stream) {
        // Insert the stream object into the map against the given address.
        if (!insertStream(address, stream)) {
            return false;
        }

        // Track the specified ROUTER identity for the address.
        if (!trackIdentity(address, identity)) {
            removeStream(address);
            return false;
        }

        return true;
    }

    /**
     * Track a particular ROUTER identity for a stream already added to the state.
     *
     * @param identity ROUTER identity of the address.
     * @param address ZeroMQ address to associate with the identity.
     *
     * @return whether the pair was inserted.
     */
    public synchronized boolean trackIdentity(ZMQAddress address, String identity) {
        if (addressesByIdentity.containsKey(identity)) {
            // The address is already present in the map.
            return false;
        }

        // The stream is not present in either map; insert it into both.
        addressesByIdentity.put(identity, address);

        return true;
    }

    /**
     * Remove a message stream from the state, referenced by its address. If present,
     * the identity associated with this address is also removed.
     *
     * @param address ZeroMQ address of the stream to remove.
     */
    public synchronized void removeStream(ZMQAddress address) {
        if (addressesByIdentity.containsValue(address)) {
            // The address has an associated identity; remove it from the map.
            String toRemove = null;
            for (Map.Entry<String, ZMQAddress> entry : addressesByIdentity.entrySet()) {
                if (Objects.equals(entry.getValue(), address)) {
                    toRemove = entry.getKey();
                    break;
                }
            }
            addressesByIdentity.remove(toRemove);
        }

        // Remove the stream from the map.
        streamsByAddress.remove(address.toCanonicalString());
    }

    /**
     * Return a reference to the message stream associated with a particular
     * ROUTER identity.
     *
     * @param identity ROUTER identity of the stream to return.
     *
     * @return a reference to a ZMQMessageStream object, or null if no such
     *         object exists for the given identity.
     */
    public synchronized ZMQMessageStream getStreamByIdentity(String identity) {
        // Return the address tracked by the identity.
        ZMQAddress address = addressesByIdentity.get(identity);
        if (address == null) {
            // The identity does not yet have an associated address.
            return null;
        }

        // Return the stream associated with the address.
        return streamsByAddress.get(address.toCanonicalString());
    }

    /**
     * Return a reference to the message stream associated with a particular
     * ZeroMQ address.
     *
     * @param address The ZeroMQ address of the stream to return.
     *
     * @return a reference to a ZMQMessageStream object, or null if no such
     *         object exists for the given address.
     */
    public synchronized ZMQMessageStream getStreamByAddress(ZMQAddress address) {
        return streamsByAddress.get(address.toCanonicalString());
    }

    public ZMQAddress getLocalAddress() {
        return localAddress;
    }
}
