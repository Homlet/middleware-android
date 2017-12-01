package uk.ac.cam.seh208.middleware.common;


/**
 * Key names for various data items in bundles. These must be common
 * as serialised bundles must be packed/unpacked on both client and
 * service-side.
 */
public class Keys {

    @SuppressWarnings("WeakerAccess")
    public static class Command {
        public static class Options {
            public static final String HOST = "HOST";
            public static final String QUERY = "QUERY";
            public static final String EXPOSED = "EXPOSED";
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class EndpointDetails {
        public static final String NAME = "NAME";
        public static final String DESC = "DESC";
        public static final String POLARITY = "POLARITY";
        public static final String SCHEMA = "SCHEMA";
        public static final String TAGS = "TAGS";
    }

    @SuppressWarnings("WeakerAccess")
    public static class Query {
        public static final String TAGS_TO_INCLUDE = "TAGS_TO_INCLUDE";
        public static final String TAGS_TO_EXCLUDE = "TAGS_TO_EXCLUDE";
        public static final String NAME_REGEX = "NAME_REGEX";
        public static final String DESC_REGEX = "DESC_REGEX";
        public static final String SCHEMA = "SCHEMA";
        public static final String POLARITY = "POLARITY";
        public static final String MATCHES = "MATCHES";
    }

    public static class MiddlewareService {
        public static final String BINDER_TYPE = "BINDER_TYPE";
        public static final String ENDPOINT_NAME = "ENDPOINT_NAME";
    }
}
