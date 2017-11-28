package uk.ac.cam.seh208.middleware.core;

import android.app.Application;

import com.google.gson.Gson;


/**
 * Singleton class maintained by the Android runtime, having the
 * lifecycle of a instance of the middleware.
 */
public class MiddlewareApplication extends Application {
    /**
     * Instance of Gson (Google's JSON [de]serialisation library).
     */
    public final Gson gson = new Gson();
}
