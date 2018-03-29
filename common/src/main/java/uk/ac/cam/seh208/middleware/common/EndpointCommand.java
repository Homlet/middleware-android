package uk.ac.cam.seh208.middleware.common;


import android.os.Parcel;

/**
 * 'Marker' class used to differentiate commands run on endpoints
 * from the more general middleware commands.
 */
public abstract class EndpointCommand extends Command {

    /**
     * This object is part of the Parcelable interface. It is used to instantiate
     * new instances of commands from serialized parcels.
     */
    public static final Creator<EndpointCommand> CREATOR = new Creator<EndpointCommand>() {
        @Override
        public EndpointCommand createFromParcel(Parcel in) {
            CommandType type = (CommandType) in.readSerializable();
            return (EndpointCommand) type.creator.createFromParcel(in);
        }

        @Override
        public EndpointCommand[] newArray(int size) {
            return new EndpointCommand[size];
        }
    };
}
