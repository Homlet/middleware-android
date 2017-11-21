package uk.ac.cam.seh208.middleware.common;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;


/**
 * Describes a filter on a set of endpoints.
 *
 * The type of set the filter operates on differs depending on
 * context: map queries are sent to peers to establish mappings
 * from the available peer endpoints; unmap queries are applied
 * to locally held data on remote mappings.
 */
public class Query implements Parcelable {

    /**
     * Key strings for storing fields within the serialisation bundle.
     */
    private static final String TAGS_TO_INCLUDE = "TAGS_TO_INCLUDE";
    private static final String TAGS_TO_EXCLUDE = "TAGS_TO_EXCLUDE";
    private static final String NAME_REGEX = "NAME_REGEX";
    private static final String DESC_REGEX = "DESC_REGEX";
    private static final String SCHEMA = "SCHEMA";
    private static final String POLARITY = "POLARITY";
    private static final String MATCHES = "MATCHES";

    /**
     * Value for matches indicating that the query should accept
     * endpoints indefinitely.
     */
    public static final int MATCH_INDEFINITELY = -1;


    /**
     * Regular expression to test against the endpoint name.
     */
    private String nameRegex;

    /**
     * Regular expression to test against the endpoint description.
     */
    private String descRegex;

    /**
     * Message schema to require the endpoint matches.
     */
    private String schema;

    /**
     * Polarity to require the endpoint has.
     */
    private Polarity polarity;

    /**
     * List of tags to require present in the endpoint.
     */
    private ArrayList<String> tagsToInclude;

    /**
     * List of tags to require not present in the endpoint.
     */
    private ArrayList<String> tagsToExclude;

    /**
     * Number of endpoints to match before rejecting others.
     */
    private int matches = MATCH_INDEFINITELY;

    /**
     * This object is part of the Parcelable interface. It is used to instantiate
     * new instances of queries from serialised parcels.
     */
    public static final Creator<Query> CREATOR = new Creator<Query>() {
        @Override
        public Query createFromParcel(Parcel in) {
            return new Query(in);
        }

        @Override
        public Query[] newArray(int size) {
            return new Query[size];
        }
    };


    public Query() {
        tagsToInclude = new ArrayList<>();
        tagsToExclude = new ArrayList<>();
    }

    protected Query(Parcel in) {
        // Read the bundle from the parcel.
        @SuppressLint("ParcelClassLoader")
        Bundle bundle = in.readBundle();

        // Extract the fields from the bundle.
        nameRegex = bundle.getString(NAME_REGEX);
        descRegex = bundle.getString(DESC_REGEX);
        schema = bundle.getString(SCHEMA);
        tagsToInclude = bundle.getStringArrayList(TAGS_TO_INCLUDE);
        if (tagsToInclude == null) {
            tagsToInclude = new ArrayList<>();
        }
        tagsToExclude = bundle.getStringArrayList(TAGS_TO_EXCLUDE);
        if (tagsToExclude == null) {
            tagsToExclude = new ArrayList<>();
        }
        matches = bundle.getInt(MATCHES, MATCH_INDEFINITELY);
    }

    public void setNameRegex(String regex) {
        nameRegex = regex;
    }

    public void unsetNameRegex() {
        nameRegex = null;
    }

    public void setDescRegex(String regex) {
        descRegex = regex;
    }

    public void unsetDescRegex() {
        descRegex = null;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public void unsetSchema() {
        schema = null;
    }

    public void setPolarity(Polarity polarity) {
        this.polarity = polarity;
    }

    public void unsetPolarity() {
        polarity = null;
    }

    public void includeTag(String tag) {
        if (tag == null) {
            // Ignore null tags.
            return;
        }

        tagsToInclude.add(tag);

        // Remove the tag from the exclude list.
        if (tagsToExclude != null) {
            tagsToExclude.remove(tag);
        }
    }

    public void includeTags(List<String> tags) {
        // Ignore null tags.
        tags.remove(null);

        tagsToInclude.addAll(tags);

        // Remove the tags from the exclude list.
        if (tagsToExclude != null) {
            tagsToExclude.removeAll(tags);
        }
    }

    public void excludeTag(String tag) {
        if (tag == null) {
            // Ignore null tags.
            return;
        }

        tagsToExclude.add(tag);

        // Remove the tag from the include list.
        if (tagsToInclude != null) {
            tagsToInclude.remove(tag);
        }
    }

    public void excludeTags(List<String> tags) {
        // Ignore null tags.
        tags.remove(null);

        tagsToExclude.addAll(tags);

        // Remove the tags from the include list.
        if (tagsToInclude != null) {
            tagsToInclude.removeAll(tags);
        }
    }

    public void ignoreTag(String tag) {
        // Remove the tag from both tag lists.
        if (tagsToInclude != null) {
            tagsToInclude.remove(tag);
        }
        if (tagsToExclude != null) {
            tagsToExclude.remove(tag);
        }
    }

    public void ignoreTags(List<String> tags) {
        // Remove the tags from both tag lists.
        if (tagsToInclude != null) {
            tagsToInclude.removeAll(tags);
        }
        if (tagsToExclude != null) {
            tagsToExclude.removeAll(tags);
        }
    }

    public void setMatches(int matches) {
        this.matches = matches;
    }

    /**
     * Return a closure which accepts or rejects endpoints based on the
     * query. A closure is used rather than a method so that a single
     * query object can generate multiple filters which each accept no
     * more than a fixed number of endpoints.
     *
     * This filter is best used in a stream pattern, in order to filter
     * out bad endpoints from a set of candidates. In this pattern,
     * queries can easily be composed.
     *
     * @return an (EndpointDetails -> boolean) filter predicate.
     */
    public Predicate<EndpointDetails> getFilter() {
        // Define closure variables for the current query state.
        final ArrayList<String> tagsToInclude = new ArrayList<>(this.tagsToInclude);
        final ArrayList<String> tagsToExclude = new ArrayList<>(this.tagsToExclude);
        final String nameRegex = this.nameRegex;
        final String descRegex = this.descRegex;
        final String schema = this.schema;
        final Polarity polarity = this.polarity;
        final int matches = this.matches;

        // Use a closure variable to keep track of the number of accepted endpoints.
        final int[] matched = { 0 };

        // Return a closure for filtering endpoints.
        return details -> {
            if (matches != MATCH_INDEFINITELY && matched[0] >= matches) {
                // If we have already met our quota of matches, reject
                // all future endpoints.
                return false;
            }

            // Test the endpoint name and description against the filter regex (if applicable).
            if (nameRegex != null && !details.getName().matches(nameRegex)) {
                return false;
            }

            if (descRegex != null) {
                if (details.getDesc() == null || !details.getDesc().matches(descRegex)) {
                    return false;
                }
            }

            // Test the schemata and polarity match.
            if (schema != null && !schema.equals(details.getSchema())) {
                return false;
            }

            if (polarity != null && details.getPolarity() != polarity) {
                return false;
            }

            // Test the endpoint tags match with our filter.
            List<String> tags = details.getTags();

            if (!details.getTags().containsAll(tagsToInclude)) {
                // The endpoint does not have all the tags that must be included.
                return false;
            }

            for (String tag : tagsToExclude) {
                if (tags.contains(tag)) {
                    // The endpoint has a tag that must be excluded.
                    return false;
                }
            }

            // Accept the endpoint (after increasing the accept count).
            matched[0]++;
            return true;
        };
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Pack the contents of the query into a new bundle.
        Bundle bundle = new Bundle();
        bundle.putString(NAME_REGEX, nameRegex);
        bundle.putString(DESC_REGEX, descRegex);
        bundle.putString(SCHEMA, schema);
        bundle.putSerializable(POLARITY, polarity);
        bundle.putStringArrayList(TAGS_TO_INCLUDE, tagsToInclude);
        bundle.putStringArrayList(TAGS_TO_EXCLUDE, tagsToExclude);
        bundle.putInt(MATCHES, matches);

        // Serialise the bundle into the parcel.
        dest.writeBundle(bundle);
    }
}
