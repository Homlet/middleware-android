package uk.ac.cam.seh208.middleware.common;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

import uk.ac.cam.seh208.middleware.core.network.Location;

import static uk.ac.cam.seh208.middleware.common.Keys.EndpointDetails.LOCATION;


/**
 * Immutable class describing an endpoint existing on a particular remote
 * host. These are used within the middleware to keep track of peer mappings,
 * and allow the user to specify particular mappings should be torn down.
 */
public class RemoteEndpointDetails extends EndpointDetails implements JSONSerializable {

    /**
     * This object is part of the Parcelable interface. It is used to instantiate
     * new instances of endpoint details from serialised parcels.
     */
    public static final Creator<RemoteEndpointDetails> CREATOR
            = new Creator<RemoteEndpointDetails>() {
        @Override
        public RemoteEndpointDetails createFromParcel(Parcel in) {
            return new RemoteEndpointDetails(in);
        }

        @Override
        public RemoteEndpointDetails[] newArray(int size) {
            return new RemoteEndpointDetails[size];
        }
    };


    /**
     * Location on which the endpoint is accessible.
     */
    private Location location;


    /**
     * Construct a new immutable remote endpoint details object with the given parameters.
     */
    public RemoteEndpointDetails(
            @JsonProperty("name") @NonNull String name,
            @JsonProperty("desc") String desc,
            @JsonProperty("polarity") Polarity polarity,
            @JsonProperty("schema") String schema,
            @JsonProperty("tags") List<String> tags,
            @JsonProperty("location") Location location) {
        super(name, desc, polarity, schema, tags);

        this.location = location;
    }

    protected RemoteEndpointDetails(Parcel in) {
        // Extract the base details from the parcel.
        super(in);

        // Read the bundle from the parcel.
        @SuppressLint("ParcelClassLoader")
        Bundle bundle = in.readBundle();

        // Extract the fields from the bundle.
        location = bundle.getParcelable(LOCATION);
    }

    /**
     * @return the location on which the endpoint is accessible.
     */
    public Location getLocation() {
        return location;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Pack the base endpoint details into the parcel.
        super.writeToParcel(dest, flags);

        // Pack the additional details.
        Bundle bundle = new Bundle();
        bundle.putParcelable(LOCATION, location);

        // Serialise the bundle into the parcel.
        dest.writeBundle(bundle);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RemoteEndpointDetails other = (RemoteEndpointDetails) obj;

        return (super.equals(obj) && Objects.equals(location, other.location));
    }
}
