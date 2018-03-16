package uk.ac.cam.seh208.middleware.api;


import android.content.Context;
import android.content.Intent;

import uk.ac.cam.seh208.middleware.common.IntentData;

/**
 * Application-facing interface for the RDC.
 */
public class RDC {

    public static void start(Context context) {
        // Create a new intent for starting the RDC service (if not already started).
        Intent intent = new Intent();
        intent.setClassName(IntentData.RDC_PACKAGE, IntentData.RDC_NAME);
        context.startService(intent);
    }

    public static void stop(Context context) {
        // Create a new intent for stopping the RDC service (if running).
        Intent intent = new Intent();
        intent.setClassName(IntentData.RDC_PACKAGE, IntentData.RDC_NAME);
        context.stopService(intent);
    }
}
