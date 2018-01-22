package uk.ac.cam.seh208.middleware.core;


/**
 * Represents an object that may at some point be permanently closed.
 */
interface Closeable {
    void close();
}
