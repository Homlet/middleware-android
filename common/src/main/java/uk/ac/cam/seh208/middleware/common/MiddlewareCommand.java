package uk.ac.cam.seh208.middleware.common;


import android.os.Parcel;

/**
 * 'Marker' class used to differentiate general middleware commands from
 * those ran on specific endpoints.
 */
public abstract class MiddlewareCommand extends Command {

    /**
     * This object is part of the Parcelable interface. It is used to instantiate
     * new instances of commands from serialized parcels.
     */
    public static final Creator<MiddlewareCommand> CREATOR = new Creator<MiddlewareCommand>() {
        @Override
        public MiddlewareCommand createFromParcel(Parcel in) {
            CommandType type = (CommandType) in.readSerializable();
            return (MiddlewareCommand) type.creator.createFromParcel(in);
        }

        @Override
        public MiddlewareCommand[] newArray(int size) {
            return new MiddlewareCommand[size];
        }
    };
}
