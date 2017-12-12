package uk.ac.cam.seh208.middleware.common;

import android.os.Bundle;
import android.os.Parcel;


/**
 * 'Marker' class used to differentiate commands ran on endpoints
 * from more general commands.
 */
public class EndpointCommand extends Command {
    /**
     * This object is part of the Parcelable interface. It is used to instantiate
     * new instances of commands from serialised parcels.
     */
    public static final Creator<EndpointCommand> CREATOR = new Creator<EndpointCommand>() {
        @Override
        public EndpointCommand createFromParcel(Parcel in) {
            return new EndpointCommand(in);
        }

        @Override
        public EndpointCommand[] newArray(int size) {
            return new EndpointCommand[size];
        }
    };


    protected EndpointCommand(CommandType type, Bundle options) {
        super(type, options);
    }

    protected EndpointCommand(Parcel in) {
        super(in);
    }
}
