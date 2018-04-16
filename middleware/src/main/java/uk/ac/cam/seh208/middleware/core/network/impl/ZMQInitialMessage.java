package uk.ac.cam.seh208.middleware.core.network.impl;

import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.cam.seh208.middleware.common.JSONSerializable;
import uk.ac.cam.seh208.middleware.core.network.Location;


/**
 * Data object storing a Harmony initial message.
 */
class ZMQInitialMessage implements JSONSerializable {

    /**
     * Location on which the owning environment is accessible.
     */
    private Location location;


    ZMQInitialMessage(@JsonProperty("location") Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}
