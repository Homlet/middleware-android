package uk.ac.cam.seh208.middleware.common;

import android.os.Bundle;
import android.os.Parcel;


/**
 * 'Marker' class used to differentiate general commands from
 * those ran on specific endpoints.
 */
public class MiddlewareCommand extends Command {
    protected MiddlewareCommand(CommandType type, Bundle options) {
        super(type, options);
    }

    protected MiddlewareCommand(Parcel in) {
        super(in);
    }
}
