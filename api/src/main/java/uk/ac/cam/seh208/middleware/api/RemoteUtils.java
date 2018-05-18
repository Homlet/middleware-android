package uk.ac.cam.seh208.middleware.api;

import android.os.DeadObjectException;
import android.os.RemoteException;


class RemoteUtils {

    interface RemoteThunk<T> {
        T get() throws RemoteException, MiddlewareDisconnectedException;
    }

    interface VoidRemoteThunk {
        void run() throws RemoteException, MiddlewareDisconnectedException;
    }

    static <T> T callSafe(RemoteThunk<T> thunk) throws MiddlewareDisconnectedException {
        try {
            return thunk.get();
        } catch (DeadObjectException e) {
            throw new MiddlewareDisconnectedException();
        } catch (RemoteException ignored) {
            // Unreachable.
            return null;
        }
    }

    static void callSafe(VoidRemoteThunk thunk) throws MiddlewareDisconnectedException {
        callSafe(() -> {
            thunk.run();
            return null;
        });
    }
}
