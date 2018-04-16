package uk.ac.cam.seh208.middleware.core.comms;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonProperty;

import java8.lang.Longs;
import uk.ac.cam.seh208.middleware.common.JSONSerializable;
import uk.ac.cam.seh208.middleware.core.network.Location;


/**
 * Data object describing an instance of the middleware having a unique
 * identifier, and residing at an updatable location.
 */
public class Middleware implements Parcelable, JSONSerializable {

    public static final Creator<Middleware> CREATOR = new Creator<Middleware>() {
        @Override
        public Middleware createFromParcel(Parcel in) {
            return new Middleware(in);
        }

        @Override
        public Middleware[] newArray(int size) {
            return new Middleware[size];
        }
    };


    /**
     * Universally unique identifier of the middleware instance. This is used
     * to identify middleware instances that may be contacted via multiple interfaces.
     */
    private long uuid;

    /**
     * Location at which the middleware message stream interface is exposed.
     */
    private Location messageLocation;

    /**
     * Location at which the middleware request stream interface is exposed.
     */
    private Location requestLocation;


    /**
     * Create a new middleware instance.
     *
     * @param uuid Universally unique identifier of the middleware instance.
     * @param messageLocation Location at which the message stream interface is exposed.
     * @param requestLocation Location at which the request stream interface is exposed.
     */
    public Middleware(
            @JsonProperty("uuid") long uuid,
            @JsonProperty("messageLocation") Location messageLocation,
            @JsonProperty("requestLocation") Location requestLocation) {
        this.uuid = uuid;
        this.messageLocation = messageLocation;
        this.requestLocation = requestLocation;
    }

    private Middleware(Parcel in) {
        uuid = in.readLong();
        messageLocation = in.readParcelable(getClass().getClassLoader());
        requestLocation = in.readParcelable(getClass().getClassLoader());
    }

    long getUUID() {
        return uuid;
    }

    Location getMessageLocation() {
        return messageLocation;
    }

    Location getRequestLocation() {
        return requestLocation;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(uuid);
        dest.writeParcelable(messageLocation, 0);
        dest.writeParcelable(requestLocation, 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Middleware other = (Middleware) obj;

        return uuid == other.uuid;
    }

    @Override
    public int hashCode() {
        return Longs.hashCode(uuid);
    }
}
