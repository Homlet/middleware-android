package uk.ac.cam.seh208.middleware.core.network.impl;

import java.util.HashMap;

import uk.ac.cam.seh208.middleware.core.network.MessageStream;


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
     * ZMQ address on which the ROUTER socket is bound.
     *
     * TODO: reference tcp://0.0.0.0:port sub-location instead.
     */
    private ZMQAddress localAddress;


    /**
     * Instantiate the address and stream maps.
     */
    ZMQMessageState(ZMQAddress localAddress) {
        streamsByIdentity = new HashMap<>();
        streamsByAddress = new HashMap<>();
        this.localAddress = localAddress;
    }

    /**
     * Insert a new message stream into the state. If the stream already exists in the
     * state, return false.
     *
     * @param address ZeroMQ address of the stream.
     * @param stream Reference to the stream to insert.
     *
     * @return whether the message stream was inserted.
     */
    synchronized boolean insertStreamByAddress(ZMQAddress address, ZMQMessageStream stream) {
        String addressString = address.toCanonicalString();

        if (streamsByAddress.containsKey(addressString)) {
            // The stream is present in the address map already.
            return false;
        }

        // Put the stream in the address map.
        streamsByAddress.put(addressString, stream);

        // Subscribe to stream closure, removing the stream by address.
        stream.subscribe(s -> removeStreamByAddress(s.getRemoteAddress()));

        return true;
    }

    /**
     * Insert a new message stream into the state, associating it with a particular ROUTER
     * identity. If that identity already has an associated stream,
     *
     * @param identity ROUTER identity of the stream.
     * @param stream Reference to the stream to insert.
     *
     * @return whether the message stream was inserted.
     */
    synchronized boolean insertStreamByIdentity(String identity, ZMQMessageStream stream) {
        if (streamsByIdentity.containsKey(identity)) {
            // The stream is present in the identity map already.
            return false;
        }

        // Put the stream in the identity map.
        streamsByIdentity.put(identity, stream);

        return true;
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

    /**
     * @return the local ZMQAddress associated with the ROUTER socket.
     */
    ZMQAddress getLocalAddress() {
        return localAddress;
    }
}
