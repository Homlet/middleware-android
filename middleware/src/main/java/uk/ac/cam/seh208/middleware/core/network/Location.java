package uk.ac.cam.seh208.middleware.core.network;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;
import uk.ac.cam.seh208.middleware.common.JSONSerializable;
import uk.ac.cam.seh208.middleware.core.exception.MalformedAddressException;
import uk.ac.cam.seh208.middleware.core.exception.NoValidAddressException;

import static uk.ac.cam.seh208.middleware.core.network.Address.getSchemeString;


/**
 * A grouping of addresses on various transports, representing the various
 * interface addresses on which a common service is accessible.
 */
public class Location implements Iterable<Address>, Parcelable, JSONSerializable {

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
     * Collection of addresses on which the middleware instance is accessible.
     */
    private List<Address> addresses;


    /**
     * Instantiate a new location with and a blank address set.
     */
    public Location() {
        this(Collections.emptyList());
    }

    /**
     * Instantiate a new location with addresses taken from a given list.
     */
    public Location(@JsonProperty("addresses") List<Address> addresses) {
        this.addresses = new ArrayList<>(addresses);
    }

    private Location(Parcel in) {
        addresses = new ArrayList<>();
        List<String> strings = in.createStringArrayList();
        for (String string : strings) {
            try {
                if (!addAddress(Address.make(string))) {
                    Log.w(getTag(), "Could not add address from location parcel: " +
                            "\"" + string + "\"");
                }
            } catch (MalformedAddressException e) {
                Log.w(getTag(), "Malformed address in location parcel: " +
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
     * Associate a group of new addresses with this location.
     *
     * @param addresses The concrete addresses.
     *
     * @return whether any addresses were successfully added.
     */
    public boolean addAddresses(Collection<Address> addresses) {
        return this.addresses.addAll(addresses);
    }

    /**
     * Associate a group of addresses stored in another location object with this location.
     *
     * @param other The other location.
     *
     * @return whether any addresses were successfully added.
     */
    public boolean addAddresses(Location other) {
        return this.addresses.addAll(other.addresses);
    }

    /**
     * Disassociate an address with this location.
     *
     * @param address The concrete address (which already exists in the address set).
     *
     * @return whether the address was successfully removed.
     */
    public boolean removeAddress(Address address) {
        return addresses.remove(address);
    }

    /**
     * Return the highest priority address.
     *
     * @return an address from the location, having the highest priority of all stored.
     *
     * @throws NoValidAddressException if the location is empty.
     */
    public Address priorityAddress()
            throws NoValidAddressException {
        return StreamSupport.stream(addresses)
                .sorted((a, b) -> b.getPriority() - a.getPriority())
                .findFirst()
                .get();
    }

    /**
     * Return the highest priority address registered for the given scheme set.
     *
     * @param schemes The set of schemes used to filter the addresses.
     *
     * @return an address of the type corresponding to the given scheme string.
     *
     * @throws NoValidAddressException if there are no addresses stored matching the query.
     */
    public Address priorityAddressForSchemes(@NonNull Set<String> schemes)
            throws NoValidAddressException {
        return StreamSupport.stream(addresses)
                .filter(a -> schemes.contains(getSchemeString(a)) )
                .sorted((a, b) -> b.getPriority() - a.getPriority())
                .findFirst()
                .get();
    }

    @NonNull
    @Override
    public Iterator<Address> iterator() {
        return addresses.iterator();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        List<String> stringAddresses = StreamSupport.stream(addresses)
                .map(Address::toCanonicalString)
                .collect(Collectors.toList());
        dest.writeStringList(stringAddresses);
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
        Location other = (Location) obj;

        return Objects.equals(new HashSet<>(addresses), new HashSet<>(other.addresses));
    }

    private String getTag() {
        return "LOCATION";
    }
}
