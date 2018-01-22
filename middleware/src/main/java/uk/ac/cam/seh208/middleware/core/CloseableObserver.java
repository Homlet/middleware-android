package uk.ac.cam.seh208.middleware.core;


/**
 * Interface for observers of closeable objects.
 */
public interface CloseableObserver<T extends Closeable> {
    void onClose(T object);
}
