package uk.ac.cam.seh208.middleware.core.network.impl;

import java.util.HashMap;


/**
 * Stores state associated with a ZMQ context.
 */
public class ZMQRequestState {

    /**
     * Responder used for handling request in the middleware layer.
     */
    private ZMQResponder responder;

    /**
     * Map of all currently maintained streams indexed by their ZeroMQ address.
     */
    private HashMap<String, ZMQRequestStream> streamsByAddress;

    /**
     * ZMQ address on which the ROUTER socket is bound.
     *
     * TODO: reference full location instead.
     */
    private ZMQAddress localAddress;


    /**
     * Instantiate the stream map.
     */
    public ZMQRequestState(ZMQResponder responder, ZMQAddress localAddress) {
        this.responder = responder;
        streamsByAddress = new HashMap<>();
        this.localAddress = localAddress;
    }

    /**
     * Insert a new stream into the state. If the stream already exists in the
     * state, return false.
     *
     * @param address ZeroMQ address of the stream to insert.
     * @param stream Reference to the stream to insert.
     *
     * @return whether the stream was inserted.
     */
    public synchronized boolean insertStream(ZMQAddress address, ZMQRequestStream stream) {
        String addressString = address.toCanonicalString();

        if (streamsByAddress.containsKey(addressString)) {
            // The stream is present in the map; the user must remove it first.
            return false;
        }

        // The stream is not present in the map; insert it.
        streamsByAddress.put(addressString, stream);

        return true;
    }

    /**
     * Remove a stream from the state.
     *
     * @param address ZeroMQ address of the stream to remove.
     */
    public synchronized void removeStream(ZMQAddress address) {
        streamsByAddress.remove(address.toCanonicalString());
    }

    /**
     * Return a reference to the stream associated with a particular
     * ZeroMQ address.
     *
     * @param address The ZeroMQ address of the stream to return.
     *
     * @return a reference to a ZMQRequestStream object, or null if no such
     *         object exists for the given address.
     */
    public synchronized ZMQRequestStream getStreamByAddress(ZMQAddress address) {
        return streamsByAddress.get(address.toCanonicalString());
    }

    public ZMQResponder getResponder() {
        return responder;
    }

    public ZMQAddress getLocalAddress() {
        return localAddress;
    }

}
