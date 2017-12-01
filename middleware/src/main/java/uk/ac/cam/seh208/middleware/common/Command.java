package uk.ac.cam.seh208.middleware.common;

import static uk.ac.cam.seh208.middleware.common.Keys.Command.Options.*;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;


/**
 * Describes a command from a limited subset of the endpoint API, to
 * be performed on a remote instance of the middleware. Such commands
 * are used to implement advanced flow control policies in pervasive
 * spaces, by allowing third parties to manage connections between
 * different devices.
 */
public class Command implements Parcelable {
    public enum CommandType {
        MAP,
        MAP_TO,
        UNMAP,
        UNMAP_FROM,
        SET_EXPOSED
    }


    /**
     * This object is part of the Parcelable interface. It is used to instantiate
     * new instances of commands from serialised parcels.
     */
    public static final Creator<Command> CREATOR = new Creator<Command>() {
        @Override
        public Command createFromParcel(Parcel in) {
            return new Command(in);
        }

        @Override
        public Command[] newArray(int size) {
            return new Command[size];
        }
    };


    /**
     * Construct and return a new command object describing a remote
     * indirect (via-RDC) mapping command, filtered based on some query.
     *
     * @param query Endpoint query to send the RDC and any returned hosts.
     *
     * @return a newly constructed command object.
     */
    public static Command newMap(Query query) {
        // Pack the query into an options bundle.
        Bundle options = new Bundle();
        options.putParcelable(QUERY, query);

        return new Command(CommandType.MAP, options);
    }

    /**
     * Construct and return a new command object describing a remote
     * direct mapping command, filtered based on some query.
     *
     * @param host Host to map to.
     * @param query Endpoint query to send the host.
     *
     * @return a newly constructed command object.
     */
    public static Command newMapTo(String host, Query query) {
        // Pack the query into an options bundle.
        Bundle options = new Bundle();
        options.putString(HOST, host);
        options.putParcelable(QUERY, query);

        return new Command(CommandType.MAP_TO, options);
    }

    /**
     * Construct and return a new command object describing a remote general
     * (all connected hosts) unmapping command, filtered based on some query.
     *
     * @param query Endpoint query for selecting mappings to drop.
     *
     * @return a newly constructed command object.
     */
    public static Command newUnmap(Query query) {
        // Pack the query into an options bundle.
        Bundle options = new Bundle();
        options.putParcelable(QUERY, query);

        return new Command(CommandType.UNMAP, options);
    }

    /**
     * Construct and return a new command object describing a remote specific
     * (single connected host) unmapping command, filtered based on some query.
     *
     * @param query Endpoint query for selecting mappings to drop.
     *
     * @return a newly constructed command object.
     */
    public static Command newUnmapFrom(String host, Query query) {
        // Pack the query into an options bundle.
        Bundle options = new Bundle();
        options.putString(HOST, host);
        options.putParcelable(QUERY, query);

        return new Command(CommandType.UNMAP_FROM, options);
    }

    /**
     * Construct and return a new command object describing a command to change
     * the exposed
     */
    public static Command newSetExposed(boolean exposed) {
        // Pack the query into an options bundle.
        Bundle options = new Bundle();
        options.putBoolean(EXPOSED, exposed);

        return new Command(CommandType.SET_EXPOSED, options);
    }


    /**
     * The type of command that should be run on the remote instance
     * of the middleware.
     */
    private CommandType type;

    /**
     * Options for the command, stored as a bundle.
     */
    private Bundle options;


    protected Command(CommandType type, Bundle options) {
        this.type = type;
        this.options = options;
    }

    @SuppressLint("ParcelClassLoader")
    protected Command(Parcel in) {
        // Read the command type from the parcel.
        type = (CommandType) in.readSerializable();

        // Read the bundle from the parcel.
        options = in.readBundle();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Serialise the object contents into the parcel.
        dest.writeSerializable(type);
        dest.writeBundle(options);
    }
}
