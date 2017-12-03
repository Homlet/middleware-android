package uk.ac.cam.seh208.middleware.common;

import android.os.Bundle;
import android.os.Parcel;


/**
 * 'Marker' class used to differentiate commands ran on endpoints
 * from more general commands.
 */
public class EndpointCommand extends Command {
    protected EndpointCommand(CommandType type, Bundle options) {
        super(type, options);
    }

    protected EndpointCommand(Parcel in) {
        super(in);
    }
}
