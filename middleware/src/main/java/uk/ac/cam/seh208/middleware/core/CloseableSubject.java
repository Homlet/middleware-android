package uk.ac.cam.seh208.middleware.core;

import java.util.ArrayList;
import java.util.List;


// TODO: change (open->close) to a file-descriptor like state machine.
/**
 * An implementation of a closeable subject, which keeps track of
 * observers and notifies them on closure.
 */
public abstract class CloseableSubject<T extends Closeable> implements Closeable {

    /**
     * Stores the current state of the object. Objects begin in the open state.
     */
    private boolean closed;  // TODO: change to state enum.

    /**
     * References all observers of the object.
     */
    private List<CloseableObserver<T>> observers;


    /**
     * Initialise the object state.
     */
    protected CloseableSubject() {
        closed = false;
        observers = new ArrayList<>();
    }

    /**
     * Subscribe a CloseableObserver to channel events.
     */
    public synchronized void subscribe(CloseableObserver<T> observer) {
        observers.add(observer);
    }

    /**
     * Atomically subscribe a ChannelObserver to channel events only if the
     * channel is currently open.
     *
     * @return whether subscription took place.
     */
    public synchronized boolean subscribeIfOpen(CloseableObserver<T> observer) {
        if (!closed) {
            subscribe(observer);
            return true;
        }

        return false;
    }

    /**
     * Unsubscribe a CloseableObserver from channel events.
     */
    public synchronized void unsubscribe(CloseableObserver<T> observer) {
        observers.remove(observer);
    }

    /**
     * Permanently close the channel, notifying the change to observers.
     */
    public synchronized void close() {
        if (closed) {
            return;
        }

        // Notify all observers of the channel closure.
        for (CloseableObserver<T> observer : observers) {
            try {
                observer.onClose((T) this);
            } catch (ClassCastException ignored) {
                // This is only reachable if the class has been extended incorrectly.
            }
        }

        // Allow observers and channels to be efficiently garbage collected by
        // explicitly closing the reference loop now no more events can be observed.
        observers.clear();

        closed = true;
    }

    public synchronized boolean isClosed() {
        return closed;
    }
}
