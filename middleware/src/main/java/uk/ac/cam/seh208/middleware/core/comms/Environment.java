package uk.ac.cam.seh208.middleware.core.comms;

/**
 * Interface for getting the local location of a stream environment.
 */
public interface Environment {
    /**
     * @return an enumeration of the local interface addresses on which this
     *         environment is accessible.
     */
    Location getLocation();
}
