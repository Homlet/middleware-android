package uk.ac.cam.seh208.middleware.core;

import junit.framework.Assert;

import org.junit.Test;

import java.util.ArrayList;


/**
 * Tests for objects implementing the Closeable interface.
 */
public class CloseableTest {
    /**
     * Dummy subclass of the CloseableSubject abstract class.
     */
    private static class DummySubject extends CloseableSubject<DummySubject> {}

    /**
     * Dummy observer of DummySubject closeables.
     */
    private static class DummyObserver implements CloseableObserver<DummySubject> {
        private int closed = 0;

        @Override
        public void onClose(DummySubject object) {
            closed++;
        }

        public int closedCount() {
            return closed;
        }
    }


    @Test
    public void testNullSubscription() {
        DummySubject subject = new DummySubject();

        // The subscription attempt should return false.
        boolean result = subject.subscribe(null);
        Assert.assertFalse(result);

        // This may throw a NullPointerException if the subject is tracking
        // a null observer reference.
        subject.close();
    }

    @Test
    public void testSingleObservation() {
        DummySubject subject = new DummySubject();
        DummyObserver observer = new DummyObserver();

        // Subscribe the observer to subject closure, then close the subject.
        subject.subscribe(observer);
        subject.close();

        // The subject should have notified the observer of its closure.
        Assert.assertEquals(1, observer.closedCount());
    }


    @Test
    public void testMultipleObservation() {
        DummySubject subject = new DummySubject();
        ArrayList<DummyObserver> observers = new ArrayList<>();

        // Create and subscribe the observers.
        for (int i = 0; i < 10; i++) {
            DummyObserver observer = new DummyObserver();
            observers.add(observer);
            subject.subscribe(observer);
        }

        // Close the subject.
        subject.close();

        // The subject should have notified the observers of its closure.
        for (DummyObserver observer : observers) {
            Assert.assertEquals(1, observer.closedCount());
        }
    }

    @Test
    public void testMultipleSubscription() {
        ArrayList<DummySubject> subjects = new ArrayList<>();
        DummyObserver observer = new DummyObserver();

        // Create the subjects, and subscribe to them.
        final int subjectCount = 10;
        for (int i = 0; i < subjectCount; i++) {
            DummySubject subject = new DummySubject();
            subject.subscribe(observer);
            subjects.add(subject);
        }

        // Close all the subjects.
        subjects.forEach(DummySubject::close);

        // Assert that the closed count equals the subject count.
        Assert.assertEquals(subjectCount, observer.closedCount());
    }
}
