package uk.ac.cam.seh208.middleware.common;

import android.os.Parcel;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;


/**
 * Command object describing a remote set RDC address command.
 */
public class SetRDCAddressCommand extends MiddlewareCommand {

    /**
     * This object is part of the Parcelable interface. It is used to instantiate
     * new instances of commands from serialized parcels.
     */
    public static final Creator<SetRDCAddressCommand> CREATOR = new Creator<SetRDCAddressCommand>() {
        @Override
        public SetRDCAddressCommand createFromParcel(Parcel in) {
            return new SetRDCAddressCommand(in);
        }

        @Override
        public SetRDCAddressCommand[] newArray(int size) {
            return new SetRDCAddressCommand[size];
        }
    };


    /**
     * Host on which the RDC resides.
     */
    private String address;


    /**
     * Construct and return a new command object describing a remote
     * set RDC address command.
     *
     * @param address Host on which the RDC resides.
     */
    public SetRDCAddressCommand(@JsonProperty("address") String address) {
        this.address = address;
    }

    private SetRDCAddressCommand(Parcel in) {
        address = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(address);
    }

    public String getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SetRDCAddressCommand other = (SetRDCAddressCommand) obj;

        return Objects.equals(address, other.address);
    }
}
