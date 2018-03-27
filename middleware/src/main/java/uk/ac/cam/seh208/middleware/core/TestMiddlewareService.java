package uk.ac.cam.seh208.middleware.core;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;


/**
 * Test wrapper on MiddlewareService allowing the test method to gain a
 * reference to the running service.
 */
public class TestMiddlewareService extends MiddlewareService {

    @Override
    public IBinder onBind(Intent intent) {
        // Ensure the start method is run.
        super.onBind(intent);

        return new LocalBinder();
    }

    public class LocalBinder extends Binder {

        public MiddlewareService getService() {
            // Return this instance of MiddlewareService so the tests can call public methods.
            return TestMiddlewareService.this;
        }
    }
}
