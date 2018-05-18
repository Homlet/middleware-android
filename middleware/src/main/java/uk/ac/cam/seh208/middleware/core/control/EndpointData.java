package uk.ac.cam.seh208.middleware.core.control;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverter;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.Polarity;


/**
 * Represents an endpoint in the persistence database.
 */
@SuppressWarnings("WeakerAccess")
@Entity(tableName = "endpoints")
class EndpointData {

    @SuppressWarnings("unused")
    static class PolarityConverter {

        @TypeConverter
        public static Polarity toPolarity(int polarity) {
            if (polarity == Polarity.SOURCE.ordinal()) {
                return Polarity.SOURCE;
            } else if (polarity == Polarity.SINK.ordinal()) {
                return Polarity.SINK;
            } else {
                throw new IllegalArgumentException("Could not recognize polarity.");
            }
        }

        @TypeConverter
        public static Integer toInteger(Polarity polarity) {
            return polarity.ordinal();
        }
    }

    @SuppressWarnings("unused")
    static class StringListConverter {

        @TypeConverter
        public static String toString(List<String> list) {
            // Create a string writer and JSON factory for serialising the list.
            StringWriter writer = new StringWriter();
            JsonFactory factory = new JsonFactory();

            try {
                // Create a JSON list generator and serialise the list.
                JsonGenerator generator = factory.createGenerator(writer);

                generator.writeStartArray();
                for (String string : list) {
                    generator.writeString(string);
                }
                generator.writeEndArray();

                return writer.toString();
            } catch (IOException e) {
                return null;
            }
        }

        @TypeConverter
        public static List<String> toList(String json) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                //noinspection unchecked
                return (List<String>) mapper.readValue(json, List.class);
            } catch (IOException e) {
                return null;
            }
        }
    }


    @PrimaryKey
    @ColumnInfo(name = "name")
    @NonNull
    private String name;

    @ColumnInfo(name = "desc")
    private String desc;

    @ColumnInfo(name = "polarity")
    @TypeConverters(PolarityConverter.class)
    private Polarity polarity;

    @ColumnInfo(name = "schema")
    private String schema;

    @ColumnInfo(name = "tags")
    @TypeConverters(StringListConverter.class)
    private List<String> tags;

    @ColumnInfo(name = "exposed")
    private boolean exposed;

    @ColumnInfo(name = "forceable")
    private boolean forceable;


    /**
     * Instantiate a new endpoint data object from an endpoint details object and
     * endpoint exposure and forceability parameters.
     */
    EndpointData(EndpointDetails details, boolean exposed, boolean forceable) {
        name = details.getName();
        desc = details.getDesc();
        polarity = details.getPolarity();
        schema = details.getSchema();
        tags = new ArrayList<>(details.getTags());
        this.exposed = exposed;
        this.forceable = forceable;
    }

    public EndpointData(@NonNull String name, String desc, Polarity polarity, String schema,
                        List<String> tags, boolean exposed, boolean forceable) {
        this.name = name;
        this.desc = desc;
        this.polarity = polarity;
        this.schema = schema;
        this.tags = tags;
        this.exposed = exposed;
        this.forceable = forceable;
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
        return tags;
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    boolean isForceable() {
        return forceable;
    }

    public void setForceable(boolean forceable) {
        this.forceable = forceable;
    }
}
