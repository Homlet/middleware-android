package uk.ac.cam.seh208.middleware.common;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Enumeration of supported mapping persistence levels. These determine
 * the length the middleware will go in attempting to reestablish
 * endpoint mappings after partial or complete failure.
 *
 * Partial failure refers to individual links within a mapping being
 * closed. Complete failure refers to all links being closed,
 * specifically when the last link does so.
 */
public enum Persistence implements Parcelable {
    /**
     * Indicate that the middleware should make no attempt to
     * reestablish the mapping on failure.
     */
    NONE,

    /**
     * Indicate that, in the case of complete failure, the mapping should
     * be reestablished by resending the original query.
     */
    RESEND_QUERY,

    /**
     * Indicate that, in the case of partial failure, the mapping should
     * be reestablished by sending a minimally modified query. The query
     * is modified in order to ensure the maximum number of open links
     * in the mapping is constant. For example, if a single link fails,
     * the modified query will only accept a single new remote endpoint.
     */
    RESEND_QUERY_INDIVIDUAL,

    /**
     * EXPERIMENTAL: Indicate that, in the case of partial failure, closed
     * links should be reopened as soon as possible with the same remote
     * endpoint on the same remote middleware instance.
     */
    EXACT;


    /**
     * This object is part of the Parcelable interface. It is used to instantiate
     * new instances of enums from serialised parcels.
     */
    public static final Creator<Persistence> CREATOR = new Creator<Persistence>() {
        @Override
        public Persistence createFromParcel(Parcel in) {
            return (Persistence) in.readSerializable();
        }

        @Override
        public Persistence[] newArray(int size) {
            return new Persistence[size];
        }
    };


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(this);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
