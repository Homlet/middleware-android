package uk.ac.cam.seh208.middleware.core.comms.impl;

import java.util.HashMap;

import uk.ac.cam.seh208.middleware.core.comms.MessageStream;


/**
 * Stores state associated with the Harmony context.
 */
class ZMQMessageState {

    /**
     * Map of all currently maintained stream addresses indexed by their unique
     * identity on the ROUTER socket.
     */
    private HashMap<String, ZMQMessageStream> streamsByIdentity;

    /**
     * Map of all currently maintained streams indexed by their ZeroMQ address.
     */
    private HashMap<String, ZMQMessageStream> streamsByAddress;


    /**
     * Instantiate the address and stream maps.
     */
    ZMQMessageState() {
        streamsByIdentity = new HashMap<>();
        streamsByAddress = new HashMap<>();
    }

    /**
     * Insert a new message stream into the state.
     *
     * @param address ZeroMQ address of the stream.
     * @param stream Reference to the stream to insert.
     */
    synchronized void insertStreamByAddress(ZMQAddress address, ZMQMessageStream stream) {
        String addressString = address.toCanonicalString();

        if (streamsByAddress.containsKey(addressString)) {
            // The stream is present in the address map already.
            return;
        }

        // Put the stream in the address map.
        streamsByAddress.put(addressString, stream);

        // Subscribe to stream closure, removing the stream by address.
        stream.subscribe(s -> removeStreamByAddress(((ZMQMessageStream) s).getRemote()));
    }

    /**
     * Insert a new message stream into the state, associating it with a particular ROUTER
     * identity.
     *
     * @param identity ROUTER identity of the stream.
     * @param stream Reference to the stream to insert.
     */
    synchronized void insertStreamByIdentity(String identity, ZMQMessageStream stream) {
        if (streamsByIdentity.containsKey(identity)) {
            // The stream is present in the identity map already.
            return;
        }

        // Put the stream in the identity map.
        streamsByIdentity.put(identity, stream);
    }

    /**
     * Remove a message stream from the state, referenced by its address.
     *
     * @param address ZeroMQ address of the stream to remove.
     */
    synchronized void removeStreamByAddress(ZMQAddress address) {
        // Remove the stream from the map.
        streamsByAddress.remove(address.toCanonicalString());
    }

    /**
     * Remove a message stream from the state, references by its ROUTER identity.
     *
     * @param identity ROUTER identity of the stream to remove.
     */
    synchronized void removeStreamByIdentity(String identity) {
        // Remove the stream from the map.
        streamsByIdentity.remove(identity);
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
    synchronized ZMQMessageStream getStreamByAddress(ZMQAddress address) {
        return streamsByAddress.get(address.toCanonicalString());
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
    synchronized ZMQMessageStream getStreamByIdentity(String identity) {
        return streamsByIdentity.get(identity);
    }

    /**
     * Close all message streams in the state.
     */
    synchronized void closeAll() {
        for (MessageStream stream : streamsByIdentity.values()) {
            stream.close();
        }

        for (MessageStream stream : streamsByAddress.values()) {
            stream.close();
        }
    }
}
