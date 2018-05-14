package uk.ac.cam.seh208.middleware.core;

import java.util.ArrayList;
import java.util.List;


/**
 * An implementation of a closeable subject, which keeps track of
 * observers and notifies them on closure.
 */
public abstract class CloseableSubject<T extends CloseableSubject<T>> implements Closeable {

    /**
     * Stores the current state of the subject. Subjects begin in the open state.
     */
    private volatile boolean closed;

    /**
     * References all observers of the subject.
     */
    private List<CloseableObserver<T>> observers;


    /**
     * Initialise the subject state.
     */
    protected CloseableSubject() {
        closed = false;
        observers = new ArrayList<>();
    }

    /**
     * Subscribe a CloseableObserver to closure events.
     *
     * @return whether subscription took place.
     */
    public synchronized boolean subscribe(CloseableObserver<T> observer) {
        if (observer == null) {
            return false;
        }

        if (observers.contains(observer)) {
            return false;
        }

        observers.add(observer);
        return true;
    }

    /**
     * Atomically subscribe a CloseableObserver to closure events only if the
     * subject is currently open.
     *
     * @return whether subscription took place.
     */
    public synchronized boolean subscribeIfOpen(CloseableObserver<T> observer) {
        if (!closed) {
            return subscribe(observer);
        }

        return false;
    }

    /**
     * Unsubscribe a CloseableObserver from closure events.
     */
    public synchronized void unsubscribe(CloseableObserver<T> observer) {
        observers.remove(observer);
    }

    /**
     * Permanently close the subject, notifying the change to observers.
     */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }

        // Notify all observers of the subject closure.
        for (CloseableObserver<T> observer : observers) {
            try {
                observer.onClose((T) this);
            } catch (ClassCastException ignored) {
                // This is only reachable if the class has been extended incorrectly.
            }
        }

        // Allow observers and subjects to be efficiently garbage collected by
        // explicitly closing the reference loop now no more events can be observed.
        observers.clear();

        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}
