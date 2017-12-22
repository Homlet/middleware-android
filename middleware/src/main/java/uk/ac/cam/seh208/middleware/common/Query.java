package uk.ac.cam.seh208.middleware.common;

import static uk.ac.cam.seh208.middleware.common.Keys.Query.*;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
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
     * Builder object for immutable queries.
     */
    public static class Builder {

        private String nameRegex;

        private String descRegex;

        private String schema;

        private Polarity polarity;

        private Set<String> tagsToInclude;

        private Set<String> tagsToExclude;

        private int matches = MATCH_INDEFINITELY;


        public Builder() {
            tagsToInclude = new TreeSet<>();
            tagsToExclude = new TreeSet<>();
        }

        public Builder setNameRegex(String nameRegex) {
            this.nameRegex = nameRegex;
            return this;
        }

        public Builder setDescRegex(String descRegex) {
            this.descRegex = descRegex;
            return this;
        }

        public Builder setSchema(String schema) {
            this.schema = schema;
            return this;
        }

        public Builder setPolarity(Polarity polarity) {
            this.polarity = polarity;
            return this;
        }

        public Builder includeTag(String tag) {
            if (tag == null) {
                // Ignore null tags.
                return this;
            }

            tagsToInclude.add(tag);

            // Remove the tag from the exclude list.
            if (tagsToExclude != null) {
                tagsToExclude.remove(tag);
            }
            return this;
        }

        public Builder includeTags(List<String> tags) {
            // Ignore null tags.
            tags.remove(null);

            tagsToInclude.addAll(tags);

            // Remove the tags from the exclude list.
            if (tagsToExclude != null) {
                tagsToExclude.removeAll(tags);
            }
            return this;
        }

        public Builder excludeTag(String tag) {
            if (tag == null) {
                // Ignore null tags.
                return this;
            }

            tagsToExclude.add(tag);

            // Remove the tag from the include list.
            if (tagsToInclude != null) {
                tagsToInclude.remove(tag);
            }
            return this;
        }

        public Builder excludeTags(List<String> tags) {
            // Ignore null tags.
            tags.remove(null);

            tagsToExclude.addAll(tags);

            // Remove the tags from the include list.
            if (tagsToInclude != null) {
                tagsToInclude.removeAll(tags);
            }
            return this;
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

        public Builder setMatches(int matches) {
            this.matches = matches;
            return this;
        }

        public Builder copy(Query query) {
            nameRegex = query.nameRegex;
            descRegex = query.descRegex;
            schema = query.schema;
            polarity = query.polarity;
            tagsToInclude.clear();
            tagsToInclude.addAll(query.tagsToInclude);
            tagsToExclude.clear();
            tagsToExclude.addAll(query.tagsToExclude);
            matches = query.matches;
            return this;
        }

        public Query build() {
            return new Query(
                    nameRegex,
                    descRegex,
                    schema,
                    polarity,
                    tagsToInclude,
                    tagsToExclude,
                    matches);
        }
    }

    /**
     * Value for matches indicating that the query should accept
     * endpoints indefinitely.
     */
    public static final int MATCH_INDEFINITELY = -1;

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


    /**
     * Regular expression to test against the endpoint name.
     */
    public final String nameRegex;

    /**
     * Regular expression to test against the endpoint description.
     */
    public final String descRegex;

    /**
     * Message schema to require the endpoint matches.
     */
    public final String schema;

    /**
     * Polarity to require the endpoint has.
     */
    public final Polarity polarity;

    /**
     * List of tags to require present in the endpoint.
     *
     * Note: this is initialised with an unmodifiable set in the constructor,
     *       so it may safely be made public without harming immutability.
     */
    public final Set<String> tagsToInclude;

    /**
     * List of tags to require not present in the endpoint.
     *
     * Note: this is initialised with an unmodifiable set in the constructor,
     *       so it may safely be made public without harming immutability.
     */
    public final Set<String> tagsToExclude;

    /**
     * Number of endpoints to match before rejecting others.
     */
    public final int matches;


    private Query(String nameRegex, String descRegex, String schema, Polarity polarity,
                  @NonNull Set<String> tagsToInclude, @NonNull Set<String> tagsToExclude,
                  int matches) {
        this.nameRegex = nameRegex;
        this.descRegex = descRegex;
        this.schema = schema;
        this.polarity = polarity;
        // Copy the input tag sets into intermediate sets, and store only
        // an unmodifiable view on these to ensure immutability.
        TreeSet<String> includes = new TreeSet<>();
        includes.addAll(tagsToInclude);
        this.tagsToInclude = Collections.unmodifiableSet(includes);
        TreeSet<String> excludes = new TreeSet<>();
        excludes.addAll(tagsToExclude);
        this.tagsToExclude = Collections.unmodifiableSet(excludes);
        this.matches = matches;
    }

    @SuppressLint("ParcelClassLoader")
    protected Query(Parcel in) {
        // Read the bundle from the parcel.
        Bundle bundle = in.readBundle();

        // Extract the fields from the bundle.
        nameRegex = bundle.getString(NAME_REGEX);
        descRegex = bundle.getString(DESC_REGEX);
        schema = bundle.getString(SCHEMA);
        polarity = (Polarity) bundle.getSerializable(POLARITY);
        ArrayList<String> includes = bundle.getStringArrayList(TAGS_TO_INCLUDE);
        if (includes == null) {
            tagsToInclude = new TreeSet<>();
        } else {
            tagsToInclude = new TreeSet<>(includes);
        }
        ArrayList<String> excludes = bundle.getStringArrayList(TAGS_TO_EXCLUDE);
        if (excludes == null) {
            tagsToExclude = new TreeSet<>();
        } else {
            tagsToExclude = new TreeSet<>(excludes);
        }
        matches = bundle.getInt(MATCHES, MATCH_INDEFINITELY);
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
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Query)) {
            return false;
        }

        final Query other = (Query) obj;
        return (Objects.equals(nameRegex, other.nameRegex)
             && Objects.equals(descRegex, other.descRegex)
             && Objects.equals(schema, other.schema)
             && polarity == other.polarity
             && Objects.equals(tagsToInclude, other.tagsToInclude)
             && Objects.equals(tagsToExclude, other.tagsToExclude)
             && matches == other.matches);
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
        bundle.putStringArrayList(TAGS_TO_INCLUDE, Lists.newArrayList(tagsToInclude));
        bundle.putStringArrayList(TAGS_TO_EXCLUDE, Lists.newArrayList(tagsToExclude));
        bundle.putInt(MATCHES, matches);

        // Serialise the bundle into the parcel.
        dest.writeBundle(bundle);
    }
}
