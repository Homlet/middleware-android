package uk.ac.cam.seh208.middleware.core.control;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverter;
import android.arch.persistence.room.TypeConverters;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import uk.ac.cam.seh208.middleware.common.JSONSerializable;
import uk.ac.cam.seh208.middleware.common.Persistence;
import uk.ac.cam.seh208.middleware.common.Polarity;
import uk.ac.cam.seh208.middleware.common.Query;

import static android.arch.persistence.room.ForeignKey.CASCADE;


/**
 * Represents a mapping in the persistence database.
 */
@SuppressWarnings("WeakerAccess")
@Entity(tableName = "mappings",
        foreignKeys = @ForeignKey(entity = EndpointData.class,
                                  parentColumns = "name",
                                  childColumns = "endpointName",
                                  onDelete = CASCADE))
class MappingData {

    @SuppressWarnings("unused")
    static class PersistenceConverter {

        @TypeConverter
        public static Persistence toPolarity(int persistence) {
            if (persistence == Persistence.NONE.ordinal()) {
                return Persistence.NONE;
            } else if (persistence == Persistence.RESEND_QUERY.ordinal()) {
                return Persistence.RESEND_QUERY;
            } else if (persistence == Persistence.RESEND_QUERY_INDIVIDUAL.ordinal()) {
                return Persistence.RESEND_QUERY_INDIVIDUAL;
            } else if (persistence == Persistence.EXACT.ordinal()) {
                return Persistence.EXACT;
            } else {
                throw new IllegalArgumentException("Could not recognize polarity.");
            }
        }

        @TypeConverter
        public static Integer toInteger(Persistence persistence) {
            return persistence.ordinal();
        }
    }

    @SuppressWarnings("unused")
    static class QueryConverter {

        @TypeConverter
        public static String toString(Query query) {
            return query.toJSON();
        }

        @TypeConverter
        public static Query toQuery(String json) {
            try {
                return JSONSerializable.fromJSON(json, Query.class);
            } catch (IOException e) {
                return null;
            }
        }
    }


    @PrimaryKey
    @ColumnInfo(name = "mappingId")
    @SuppressWarnings("unused")
    private long mappingId;

    @ColumnInfo(name = "endpointName")
    @SuppressWarnings("unused")
    private String endpointName;

    @ColumnInfo(name = "query")
    @TypeConverters(QueryConverter.class)
    private Query query;

    @ColumnInfo(name = "persistence")
    @TypeConverters(PersistenceConverter.class)
    private Persistence persistence;


    /**
     * Instantiate a new mapping data object from a parent endpoint name, a query,
     * and a persistence policy.
     */
    public MappingData(long mappingId, String endpointName, Query query, Persistence persistence) {
        this.mappingId = mappingId;
        this.endpointName = endpointName;
        this.query = query;
        this.persistence = persistence;
    }

    public long getMappingId() {
        return mappingId;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public Query getQuery() {
        return query;
    }

    public Persistence getPersistence() {
        return persistence;
    }
}
