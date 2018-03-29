package uk.ac.cam.seh208.middleware.common;

import android.os.Parcel;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;


/**
 * Command object describing a remote direct mapping command,
 * filtered based on some query.
 */
public class MapToCommand extends EndpointCommand {

    /**
     * This object is part of the Parcelable interface. It is used to instantiate
     * new instances of commands from serialized parcels.
     */
    public static final Creator<MapToCommand> CREATOR = new Creator<MapToCommand>() {
        @Override
        public MapToCommand createFromParcel(Parcel in) {
            return new MapToCommand(in);
        }

        @Override
        public MapToCommand[] newArray(int size) {
            return new MapToCommand[size];
        }
    };

    /**
     * Host to map to.
     */
    private String address;

    /**
     * Endpoint query to send the RDC and any returned hosts.
     */
    private Query query;

    /**
     * Persistence value for the mapping.
     */
    private Persistence persistence;


    /**
     * Construct and return a new command object describing a remote
     * direct mapping command, filtered based on some query.
     *
     * @param address Host to map to.
     * @param query Endpoint query to send the host.
     * @param persistence Persistence value for the mapping.
     */
    public MapToCommand(
            @JsonProperty("address") String address,
            @JsonProperty("query") Query query,
            @JsonProperty("persistence") Persistence persistence) {
        this.address = address;
        this.query = query;
        this.persistence = persistence;
    }

    private MapToCommand(Parcel in) {
        address = in.readString();
        query = in.readParcelable(getClass().getClassLoader());
        persistence = (Persistence) in.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(address);
        dest.writeParcelable(query, 0);
        dest.writeSerializable(persistence);
    }

    public String getAddress() {
        return address;
    }

    public Query getQuery() {
        return query;
    }

    public Persistence getPersistence() {
        return persistence;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MapToCommand other = (MapToCommand) obj;

        return (Objects.equals(address, other.address) &&
                Objects.equals(query, other.query) &&
                persistence == other.persistence);
    }
}
