package uk.ac.cam.seh208.middleware.api;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import uk.ac.cam.seh208.middleware.api.exception.MiddlewareDisconnectedException;
import uk.ac.cam.seh208.middleware.binder.ICombined;


class MiddlewareServiceConnection implements ServiceConnection {

    private static final int TIMEOUT = 20000;

    private ICombined binder;


    @NonNull
    public synchronized ICombined waitForBinder() throws MiddlewareDisconnectedException {
        if (binder == null) {
            try {
                wait(TIMEOUT);
            } catch (InterruptedException ignored) {
                // Do nothing.
            }
        }

        if (binder == null) {
            throw new MiddlewareDisconnectedException();
        }

        return binder;
    }

    @Override
    public synchronized void onServiceConnected(ComponentName name, IBinder service) {
        binder = ICombined.Stub.asInterface(service);

        // Notify that the middleware has connected.
        notifyAll();
    }

    @Override
    public synchronized void onServiceDisconnected(ComponentName name) {
        Log.e(getTag(), "Middleware service unexpectedly disconnected from client.");
        binder = null;
    }

    private static String getTag() {
        return "MW_CONN";
    }
}
