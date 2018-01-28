package uk.ac.cam.seh208.middleware.core.network;

import java.util.HashMap;


// TODO: share superclass with ZMQRequestState.
/**
 * Stores state associated with the Harmony context.
 */
public class HarmonyState {

    // TODO: allow storage of streams without associated identity.
    /**
     * Map of all currently maintained streams indexed by their unique
     * identity on the ROUTER socket.
     */
    private HashMap<String, HarmonyMessageStream> streamsByIdentity;

    /**
     * Map of all currently maintained streams indexed by their ZeroMQ address.
     */
    private HashMap<String, HarmonyMessageStream> streamsByAddress;

    /**
     * ZMQ address on which the ROUTER socket is bound.
     *
     * TODO: reference full location instead.
     */
    private ZMQAddress localAddress;


    /**
     * Instantiate the stream maps.
     */
    public HarmonyState(ZMQAddress localAddress) {
        streamsByIdentity = new HashMap<>();
        streamsByAddress = new HashMap<>();
        this.localAddress = localAddress;
    }

    /**
     * Insert a new message stream into the state. If the stream already exists in the
     * state, return false. If the stream exists in one internal HashMap but not the
     * other, BadHarmonyStateException is thrown.
     *
     * @param identity ROUTER identity of the stream to insert.
     * @param address ZeroMQ address of the stream to insert.
     * @param stream Reference to the stream to insert.
     *
     * @return whether the message stream was inserted.
     */
    public synchronized boolean insertStream(String identity, ZMQAddress address,
                                             HarmonyMessageStream stream) {
        boolean keyPresent = streamsByIdentity.containsKey(identity);
        String addressString = address.toCanonicalString();

        if (keyPresent ^ streamsByAddress.containsKey(addressString)) {
            // One list contains the key already. Since we cannot have a stream which
            // shares a ROUTER identity but not an address (or vice-verse) with
            // another this case should be unreachable.
            throw new BadStateException();
        }

        if (keyPresent) {
            // The stream is present in both maps; the user must remove it first.
            return false;
        }

        // The stream is not present in either map; insert it into both.
        streamsByIdentity.put(identity, stream);
        streamsByAddress.put(addressString, stream);

        return true;
    }

    /**
     * Remove a message stream from the state. The stream must be correctly referenced
     * by both identity and host.
     *
     * @param identity ROUTER identity of the stream to remove.
     * @param address ZeroMQ address of the stream to remove.
     */
    public synchronized void removeStream(String identity, ZMQAddress address) {
        String addressString = address.toCanonicalString();

        if (streamsByIdentity.containsKey(identity) ^ streamsByAddress.containsKey(addressString)) {
            // One list contains the key already. Since we cannot have a stream which
            // shares a ROUTER identity but not an address (or vice-verse) with
            // another this case should be unreachable.
            throw new BadStateException();
        }

        if (streamsByIdentity.get(identity) != streamsByAddress.get(addressString)) {
            throw new IllegalArgumentException(
                    "Objects referenced by identity and host must match.");
        }

        // Remove the stream from both lists.
        streamsByIdentity.remove(identity);
        streamsByAddress.remove(addressString);
    }

    /**
     * Return a reference to the message stream associated with a particular
     * ROUTER identity.
     *
     * @param identity ROUTER identity of the stream to return.
     *
     * @return a reference to a HarmonyMessageStream object, or null if no such
     *         object exists for the given identity.
     */
    public synchronized HarmonyMessageStream getStreamByIdentity(String identity) {
        return streamsByIdentity.get(identity);
    }

    /**
     * Return a reference to the message stream associated with a particular
     * ZeroMQ address.
     *
     * @param address The ZeroMQ address of the stream to return.
     *
     * @return a reference to a HarmonyMessageStream object, or null if no such
     *         object exists for the given address.
     */
    public synchronized HarmonyMessageStream getStreamByAddress(ZMQAddress address) {
        return streamsByAddress.get(address.toCanonicalString());
    }

    public ZMQAddress getLocalAddress() {
        return localAddress;
    }
}
