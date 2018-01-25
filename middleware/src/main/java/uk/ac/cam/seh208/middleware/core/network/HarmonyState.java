package uk.ac.cam.seh208.middleware.core.network;

import java.util.HashMap;


/**
 * Stores state associated with the Harmony context.
 */
public class HarmonyState {
    /**
     * Map of all currently maintained streams indexed by their unique
     * identity on the ROUTER socket.
     */
    private HashMap<String, HarmonyMessageStream> streamsByIdentity;

    /**
     * Map of all currently maintained streams indexed by their host name.
     */
    private HashMap<String, HarmonyMessageStream> streamsByHost;


    /**
     * Instantiate the stream maps.
     */
    public HarmonyState() {
        streamsByIdentity = new HashMap<>();
        streamsByHost = new HashMap<>();
    }

    /**
     * Insert a new message stream into the state. If the stream already exists in the
     * state, return false. If the stream exists in one internal HashMap but not the
     * other, BadHarmonyStateException is thrown.
     *
     * @param identity ROUTER identity of the stream to insert.
     * @param host Remote host of the stream to insert.
     * @param stream Reference to the stream to insert.
     *
     * @return whether the message stream was inserted.
     *
     * @throws BadHarmonyStateException if the operation finds the object in a bad state.
     */
    public synchronized boolean insertStream(String identity, String host,
                                             HarmonyMessageStream stream)
            throws BadHarmonyStateException {
        boolean keyPresent = streamsByIdentity.containsKey(identity);

        if (keyPresent ^ streamsByHost.containsKey(host)) {
            // One list contains the key already. Since we cannot have a stream which
            // shares a ROUTER identity but not a remote host (or vice-verse) with
            // another this case should be unreachable.
            throw new BadHarmonyStateException();
        }

        if (keyPresent) {
            // The stream is present in both maps; the user must remove it first.
            return false;
        }

        // The stream is not present in either map; insert it into both.
        streamsByIdentity.put(identity, stream);
        streamsByHost.put(host, stream);

        return true;
    }

    /**
     * Remove a message stream from the state. The stream must be correctly referenced
     * by both identity and host.
     *
     * @param identity ROUTER identity of the stream to remove.
     * @param host Remote host of the stream to remove.
     *
     * @throws BadHarmonyStateException if the operation finds the object in a bad state.
     */
    public synchronized void removeStream(String identity, String host)
            throws BadHarmonyStateException {
        if (streamsByIdentity.containsKey(identity) ^ streamsByHost.containsKey(host)) {
            // One list contains the key already. Since we cannot have a stream which
            // shares a ROUTER identity but not a remote host (or vice-verse) with
            // another this case should be unreachable.
            throw new BadHarmonyStateException();
        }

        if (streamsByIdentity.get(identity) != streamsByHost.get(host)) {
            throw new IllegalArgumentException(
                    "Objects referenced by identity and host must match.");
        }

        // Remove the stream from both lists.
        streamsByIdentity.remove(identity);
        streamsByHost.remove(host);
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
     * remote host.
     *
     * @param host Remote host of the stream to return.
     *
     * @return a reference to a HarmonyMessageStream object, or null if no such
     *         object exists for the given host.
     */
    public synchronized HarmonyMessageStream getStreamByHost(String host) {
        return streamsByHost.get(host);
    }
}
