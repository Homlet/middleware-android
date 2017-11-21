package uk.ac.cam.seh208.middleware.common;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;

import java.util.List;


/**
 * Immutable class describing an endpoint existing on a particular remote
 * host. These are used within the middleware to keep track of peer mappings,
 * and allow the user to specify particular mappings should be torn down.
 */
public class RemoteEndpointDetails extends EndpointDetails {

    /**
     * Key strings for storing fields within the serialisation bundle.
     */
    private static final String HOST = "HOST";
    private static final String PORT = "PORT";


    /**
     * String representation of the remote host, as would be recognised
     * by the Java sockets library.
     */
    private String host;

    /**
     * Port number of the peer middleware instance.
     */
    private int port;

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
     * Construct a new immutable remote endpoint details object with the given parameters.
     */
    public RemoteEndpointDetails(@NonNull String name, String desc, Polarity polarity,
                                 String schema, List<String> tags, String host) {
        // TODO: move this magic number somewhere sensible.
        this(name, desc, polarity, schema, tags, host, 1768);
    }

    public RemoteEndpointDetails(@NonNull String name, String desc, Polarity polarity,
                                 String schema, List<String> tags, String host, int port) {
        super(name, desc, polarity, schema, tags);

        this.host = host;
        this.port = port;
    }

    protected RemoteEndpointDetails(Parcel in) {
        // Extract the base details from the parcel.
        super(in);

        // Read the bundle from the parcel.
        @SuppressLint("ParcelClassLoader")
        Bundle bundle = in.readBundle();

        // Extract the fields from the bundle.
        host = bundle.getString(HOST);
        port = bundle.getInt(PORT);
    }

    /**
     * @return the remote host string.
     */
    public String getHost() {
        return host;
    }

    /**
     * @return the port number of the peer middleware instance.
     */
    public int getPort() {
        return port;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Pack the base endpoint details into the parcel.
        super.writeToParcel(dest, flags);

        // Pack the additional details.
        Bundle bundle = new Bundle();
        bundle.putString(HOST, host);
        bundle.putInt(PORT, port);

        // Serialise the bundle into the parcel.
        dest.writeBundle(bundle);
    }
}
