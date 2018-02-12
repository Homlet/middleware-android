package uk.ac.cam.seh208.middleware.common;

import static uk.ac.cam.seh208.middleware.common.Keys.EndpointDetails.*;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;


/**
 * Immutable class describing an endpoint. This class may refer to an
 * endpoint in general; a subclass exists for describing endpoints existing
 * on a particular remote host.
 *
 * @see RemoteEndpointDetails
 */
public class EndpointDetails implements Parcelable {

    /**
     * The name of the endpoint. This is used to uniquely identify the endpoint
     * within a given middleware instance.
     */
    @NonNull
    private String name;

    /**
     * A description of the endpoint. This allows a human-readable description to
     * be attached to endpoints for debugging purposes.
     */
    private String desc;

    /**
     * The polarity of the endpoint. This value describes the direction and mode
     * in which data flows through the endpoint.
     */
    private Polarity polarity;

    /**
     * The schema string of the endpoint. This is a valid JSON-schema-formatted
     * string describing the format of message that this endpoint handles.
     */
    private String schema;

    /**
     * A set of string tags associated with this endpoint, used for querying.
     */
    private ArrayList<String> tags;

    /**
     * This object is part of the Parcelable interface. It is used to instantiate
     * new instances of endpoint details from serialised parcels.
     */
    public static final Creator<EndpointDetails> CREATOR = new Creator<EndpointDetails>() {
        @Override
        public EndpointDetails createFromParcel(Parcel in) {
            return new EndpointDetails(in);
        }

        @Override
        public EndpointDetails[] newArray(int size) {
            return new EndpointDetails[size];
        }
    };


    /**
     * Construct a new immutable endpoint details object with the given parameters.
     */
    public EndpointDetails(@NonNull String name, String desc, Polarity polarity,
                           String schema, List<String> tags) {
        // TODO: validate schema string.

        this.name = name;
        this.desc = desc;
        this.polarity = polarity;
        this.schema = schema;
        if (tags != null) {
            this.tags = new ArrayList<>(new HashSet<>(tags));
        } else {
            this.tags = new ArrayList<>();
        }
    }

    protected EndpointDetails(Parcel in) {
        // Read the bundle from the parcel.
        @SuppressLint("ParcelClassLoader")
        Bundle bundle = in.readBundle();

        // Extract the fields from the bundle.
        //noinspection ConstantConditions
        name = bundle.getString(NAME);
        desc = bundle.getString(DESC);
        polarity = (Polarity) bundle.getSerializable(POLARITY);
        schema = bundle.getString(SCHEMA);
        tags = bundle.getStringArrayList(TAGS);
        if (tags == null) {
            tags = new ArrayList<>();
        }
    }

    @NonNull
    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public Polarity getPolarity() {
        return polarity;
    }

    public String getSchema() {
        return schema;
    }

    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Pack the endpoint details into a new bundle.
        Bundle bundle = new Bundle();
        bundle.putString(NAME, name);
        bundle.putString(DESC, desc);
        bundle.putSerializable(POLARITY, polarity);
        bundle.putString(SCHEMA, schema);
        bundle.putStringArrayList(TAGS, tags);

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
        EndpointDetails other = (EndpointDetails) obj;

        return (Objects.equals(name, other.name)
             && Objects.equals(desc, other.desc)
             && polarity == other.polarity
             && Objects.equals(schema, other.schema)
             && Objects.equals(new HashSet<>(tags), new HashSet<>(other.tags)));
    }
}
