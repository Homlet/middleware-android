package uk.ac.cam.seh208.middleware.core.network;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import java8.lang.Longs;
import uk.ac.cam.seh208.middleware.common.JSONSerializable;
import uk.ac.cam.seh208.middleware.core.exception.MalformedAddressException;


/**
 * Identifier for a unique instance of the middleware, accessible on various
 * addresses on different interfaces.
 */
public class Location implements Parcelable, JSONSerializable {

    /**
     * This object is part of the Parcelable interface. It is used to instantiate
     * new instances of locations from serialized parcels.
     */
    public static final Creator<Location> CREATOR = new Creator<Location>() {
        @Override
        public Location createFromParcel(Parcel in) {
            return new Location(in);
        }

        @Override
        public Location[] newArray(int size) {
            return new Location[size];
        }
    };

    /**
     * Universally unique identifier of the middleware instance. This is used
     * to identify middleware instances that may be contacted via multiple interfaces.
     */
    private long uuid;

    /**
     * Collection of addresses on which the middleware instance is accessible.
     */
    private List<Address> addresses;


    /**
     * Instantiate a new location with the given uuid and a blank address set.
     */
    public Location(@JsonProperty("uuid") long uuid) {
        this.uuid = uuid;
        addresses = new ArrayList<>();
    }

    private Location(Parcel in) {
        uuid = in.readLong();
        addresses = new ArrayList<>();
        List<String> strings = in.createStringArrayList();
        for (String string : strings) {
            try {
                if (!addAddress(Address.make(string))) {
                    Log.w(getTag(), "Could not add address to location: " +
                            "\"" + string + "\"");
                }
            } catch (MalformedAddressException e) {
                Log.e(getTag(), "Error parsing address from location parcel: " +
                        "\"" + string + "\"");
            }
        }
    }

    /**
     * Associate a new address with this location.
     *
     * @param address The concrete address.
     *
     * @return whether the address was successfully added.
     */
    public boolean addAddress(Address address) {
        return !addresses.contains(address) && addresses.add(address);
    }

    /**
     * Disassociate an address with this location.
     *
     * @param address The conrete address (which already exists in the address set).
     *
     * @return whether the address was successfully removed.
     */
    public boolean removeAddress(Address address) {
        return addresses.remove(address);
    }

    /**
     * @return an unmodifiable list of all addresses associated with this location.
     */
    public List<Address> getAddresses() {
        return Collections.unmodifiableList(addresses);
    }


    public long getUUID() {
        return uuid;
    }

    /**
     * Return a string encoding of the location.
     *
     * @return a reference to a String object.
     */
    @Override
    public String toString() {
        // Use a builder to prevent unnecessary string copying.
        StringBuilder builder = new StringBuilder();

        // Begin the string with the UUID.
        builder.append(Long.toString(uuid, 16));

        // Append all serialised addresses.
        for (Address address : addresses) {
            builder.append("|");
            builder.append(address);
        }

        // Build the output string.
        return builder.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(uuid);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Location other = (Location) obj;

        return (uuid == other.uuid
             && Objects.equals(addresses, other.addresses));  // TODO: reintroduce HashSets.
    }

    @Override
    public int hashCode() {
        return Longs.hashCode(uuid);
    }

    private String getTag() {
        return "LOCATION";
    }
}
