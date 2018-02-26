package uk.ac.cam.seh208.middleware.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;


public interface JSONSerializable {

    /**
     * @return a newly constructed instance of the given implementor class,
     *         parsed from the given JSON string.
     *
     * @throws IOException when the given JSON string is malformed, or does
     *                     not match the schema of the class being constructed.
     */
    static <T extends JSONSerializable> T fromJSON(String string, Class<T> type)
            throws IOException {
        return new ObjectMapper().readValue(string, type);
    }

    /**
     * @return a JSON string representation of the object.
     */
    default String toJSON() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
