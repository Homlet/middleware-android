package uk.ac.cam.seh208.middleware.common;

import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


/**
 * Describes a command from a limited subset of the endpoint API, to
 * be performed on a remote instance of the middleware. Such commands
 * are used to implement advanced flow control policies in pervasive
 * spaces, by allowing third parties to manage connections between
 * different devices.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "tag"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MapCommand.class, name = "MAP"),
        @JsonSubTypes.Type(value = MapToCommand.class, name = "MAP_TO"),
        @JsonSubTypes.Type(value = UnmapAllCommand.class, name = "UNMAP_ALL"),
        @JsonSubTypes.Type(value = CloseAllCommand.class, name = "CLOSE_ALL"),
        @JsonSubTypes.Type(value = SetRDCAddressCommand.class, name = "SET_RDC_ADDRESS")
})
public abstract class Command implements Parcelable, JSONSerializable {

    protected enum CommandType {
        MAP(MapCommand.class),
        MAP_TO(MapToCommand.class),
        UNMAP_ALL(UnmapAllCommand.class),
        CLOSE_ALL(CloseAllCommand.class),
        SET_RDC_ADDRESS(SetRDCAddressCommand.class);


        public Creator<? extends Command> creator;


        CommandType(Class<? extends Command> clazz) {
            try {
                //noinspection unchecked
                creator = (Creator<? extends Command>) clazz.getField("CREATOR").get(null);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                // This should only be reachable in the case of a programming error.
                e.printStackTrace();
            }
        }
    }


    @Override
    public int describeContents() {
        return 0;
    }
}
