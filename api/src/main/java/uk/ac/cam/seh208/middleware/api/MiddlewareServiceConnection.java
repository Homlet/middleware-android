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


    /**
     * Binder passed from the OS for communication with the middleware service.
     */
    private ICombined binder;

    /**
     * One-shot callback to be run when the service connects the first time.
     */
    private Runnable callback;


    @NonNull
    synchronized ICombined waitForBinder() throws MiddlewareDisconnectedException {
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
        Log.i(getTag(), "Middleware service connected to client.");
        binder = ICombined.Stub.asInterface(service);

        // Notify that the middleware has connected.
        notifyAll();

        // Run the callback (if applicable).
        if (callback != null) {
            callback.run();
            callback = null;
        }
    }

    @Override
    public synchronized void onServiceDisconnected(ComponentName name) {
        Log.e(getTag(), "Middleware service unexpectedly disconnected from client.");
        binder = null;
    }

    void setCallback(Runnable callback) {
        this.callback = callback;
    }

    private static String getTag() {
        return "MW_CONN";
    }
}
