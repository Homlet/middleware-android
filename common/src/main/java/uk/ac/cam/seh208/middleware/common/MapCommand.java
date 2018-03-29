package uk.ac.cam.seh208.middleware.common;

import android.os.Parcel;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;


/**
 * Command object describing a remote indirect (via-RDC) mapping command,
 * filtered based on some query.
 */
public class MapCommand extends EndpointCommand {

    /**
     * This object is part of the Parcelable interface. It is used to instantiate
     * new instances of commands from serialized parcels.
     */
    public static final Creator<MapCommand> CREATOR = new Creator<MapCommand>() {
        @Override
        public MapCommand createFromParcel(Parcel in) {
            return new MapCommand(in);
        }

        @Override
        public MapCommand[] newArray(int size) {
            return new MapCommand[size];
        }
    };


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
     * indirect (via-RDC) mapping command, filtered based on some query.
     *
     * @param query Endpoint query to send the RDC and any returned hosts.
     * @param persistence Persistence value for the mapping.
     */
    public MapCommand(
            @JsonProperty("query") Query query,
            @JsonProperty("persistence") Persistence persistence) {
        this.query = query;
        this.persistence = persistence;
    }

    private MapCommand(Parcel in) {
        query = in.readParcelable(getClass().getClassLoader());
        persistence = (Persistence) in.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(query, 0);
        dest.writeSerializable(persistence);
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
        MapCommand other = (MapCommand) obj;

        return (Objects.equals(query, other.query) &&
                persistence == other.persistence);
    }
}
