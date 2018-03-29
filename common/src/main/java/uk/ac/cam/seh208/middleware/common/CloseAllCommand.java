package uk.ac.cam.seh208.middleware.common;

import android.os.Parcel;


/**
 * Command object describing a remote close-all-channels command.
 */
public class CloseAllCommand extends EndpointCommand {

    /**
     * This object is part of the Parcelable interface. It is used to instantiate
     * new instances of commands from serialized parcels.
     */
    public static final Creator<CloseAllCommand> CREATOR = new Creator<CloseAllCommand>() {
        @Override
        public CloseAllCommand createFromParcel(Parcel in) {
            return new CloseAllCommand();
        }

        @Override
        public CloseAllCommand[] newArray(int size) {
            return new CloseAllCommand[size];
        }
    };


    /**
     * Construct and return a new command object describing a remote
     * close-all-channels command.
     */
    public CloseAllCommand() { }

    @Override
    public void writeToParcel(Parcel dest, int flags) { }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return true;
    }
}
