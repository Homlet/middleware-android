package uk.ac.cam.seh208.middleware.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.ac.cam.seh208.middleware.common.Persistence;
import uk.ac.cam.seh208.middleware.common.Query;
import uk.ac.cam.seh208.middleware.common.exception.IncompleteBuildException;


/**
 * Object storing the result of a mapping command called on an endpoint.
 *
 * Consists of a collections of channels from the local (near) endpoint
 * to other local/remote (far) endpoints.
 *
 * The persistence level determines how the mapping handles failure; partial
 * failure is the closure of some channels within the mapping, while complete
 * failure is the closure of all channels, or the local middleware instance
 * being killed by the scheduler.
 */
public class Mapping {

    public static class Builder {

        private Query query;

        private Persistence persistence;

        private List<Channel> channels;


        public Builder() {
            persistence = Persistence.NONE;
            channels = new ArrayList<>();
        }

        public Builder setQuery(Query query) {
            this.query = query;
            return this;
        }

        public Builder setPersistence(Persistence persistence) {
            this.persistence = persistence;
            return this;
        }

        public Builder addChannel(Channel channel) {
            channels.add(channel);
            return this;
        }

        public Builder addChannels(List<Channel> channels) {
            this.channels.addAll(channels);
            return this;
        }

        public Mapping build() throws IncompleteBuildException {
            if (query == null) {
                throw new IncompleteBuildException(Mapping.class);
            }
            return new Mapping(query, persistence, channels);
        }
    }


    public final Query query;

    public final Persistence persistence;

    public final List<Channel> channels;


    private Mapping(Query query, Persistence persistence, List<Channel> channels) {
        this.query = query;
        this.persistence = persistence;
        List<Channel> channelsInternal = new ArrayList<>(channels);
        this.channels = Collections.unmodifiableList(channelsInternal);
    }
}
