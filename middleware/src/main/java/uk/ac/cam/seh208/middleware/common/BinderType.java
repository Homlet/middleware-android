package uk.ac.cam.seh208.middleware.common;


/**
 * Enumeration of the types of binder available from the middleware
 * service. These correspond to the different interfaces defined in
 * AIDL.
 */
public enum BinderType {
    /**
     * The standard control interface.
     */
    MIDDLEWARE,

    /**
     * The interface exposed by an existing endpoint for mapping and
     * sending/receiving messages.
     */
    ENDPOINT
}
