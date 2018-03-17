package uk.ac.cam.seh208.middleware.core.network.impl;

import uk.ac.cam.seh208.middleware.core.network.Address;
import uk.ac.cam.seh208.middleware.core.network.SchemeConfiguration;


public class ZMQSchemeConfiguration implements SchemeConfiguration {

    public static final int DEFAULT_MESSAGE_PORT = 4852;

    public static final int DEFAULT_REQUEST_PORT = 4853;

    public static final int DEFAULT_RDC_PORT = 4854;


    private int port;


    public ZMQSchemeConfiguration(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String getScheme() {
        return Address.SCHEME_ZMQ;
    }
}
