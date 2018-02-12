package uk.ac.cam.seh208.middleware.common;

import android.os.Bundle;
import android.os.Parcel;


/**
 * 'Marker' class used to differentiate general commands from
 * those ran on specific endpoints.
 */
public class MiddlewareCommand extends Command {

    /**
     * This object is part of the Parcelable interface. It is used to instantiate
     * new instances of commands from serialized parcels.
     */
    public static final Creator<MiddlewareCommand> CREATOR = new Creator<MiddlewareCommand>() {
        @Override
        public MiddlewareCommand createFromParcel(Parcel in) {
            return new MiddlewareCommand(in);
        }

        @Override
        public MiddlewareCommand[] newArray(int size) {
            return new MiddlewareCommand[size];
        }
    };


    protected MiddlewareCommand(CommandType type, Bundle options) {
        super(type, options);
    }

    protected MiddlewareCommand(Parcel in) {
        super(in);
    }
}
