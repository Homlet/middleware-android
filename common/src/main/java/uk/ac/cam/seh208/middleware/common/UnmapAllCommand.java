package uk.ac.cam.seh208.middleware.common;

import android.os.Parcel;

import java.util.Objects;


/**
 * Command object describing a remote unmapping command.
 */
public class UnmapAllCommand extends EndpointCommand {

    /**
     * This object is part of the Parcelable interface. It is used to instantiate
     * new instances of commands from serialized parcels.
     */
    public static final Creator<UnmapAllCommand> CREATOR = new Creator<UnmapAllCommand>() {
        @Override
        public UnmapAllCommand createFromParcel(Parcel in) {
            return new UnmapAllCommand();
        }

        @Override
        public UnmapAllCommand[] newArray(int size) {
            return new UnmapAllCommand[size];
        }
    };


    /**
     * Construct and return a new command object describing a remote
     * unmapping command.
     */
    public UnmapAllCommand() { }

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
