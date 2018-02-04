package uk.ac.cam.seh208.middleware.core;


/**
 * Represents an object that may at some point be permanently closed.
 */
public interface Closeable {
    void close();
    boolean isClosed();
}
