package uk.ac.cam.seh208.middleware.core.comms.impl;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.cam.seh208.middleware.core.exception.MalformedAddressException;
import uk.ac.cam.seh208.middleware.core.comms.Address;
import uk.ac.cam.seh208.middleware.core.comms.AddressBuilder;


/**
 * Implementation of a ZeroMQ address.
 */
public class ZMQAddress extends Address {

    public static class Builder implements AddressBuilder {

        private int priority;

        private String host;

        private int port;


        public Builder() {
            priority = 0;
            host = "*";
            port = 0;
        }

        public Builder setHost(String host) {
            if (host.startsWith("127.")) {
                // This is a bit hacky, but prevents local loopback addresses
                // leaking to peers.
                priority = -8;
            }
            if (host.startsWith("192.168.")) {
                // This is a bit hacky, but prevents local network addresses
                // leaking to peers on other networks. This shouldn't be a massive
                // issue since in the main use case of the middleware peers only
                // communicate within the same network.
                priority = -4;
            }
            this.host = host;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        /**
         * Load the host and port from a given string formatted as a ZeroMQ
         * address (i.e. an endpoint without the transport).
         *
         * @param string The address string.
         *
         * @return a reference to this.
         */
        @Override
        public Builder fromString(String string) throws MalformedAddressException {
            // Define regular expressions for IPv4, IPv6, and hostname addresses.
            String ipv4Expr = "(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d+|\\*)";
            String ipv6Expr = "\\[([a-zA-Z0-9:]+)]:(\\d+|\\*)";
            String hostnameExpr = "([\\w.\\-]+):(\\d+|\\*)";
            String wildcardExpr = "(\\*):(\\d+|\\*)";

            // Merge the patterns into a single matcher for the input string.
            Pattern pattern = Pattern.compile(
                    ipv4Expr + "|" + ipv6Expr + "|" + hostnameExpr + "|" + wildcardExpr);
            Matcher matcher = pattern.matcher(string);

            // Match the
            if (matcher.matches()) {
                // Search the capture groups for the data (if it exists).
                for (int group = 1; group < 9; group += 2) {
                    if (matcher.group(group) == null) {
                        // We haven't found the data yet.
                        continue;
                    }

                    // Set the address parts from the matched string.
                    setHost(matcher.group(group));
                    if (matcher.group(group + 1).equals("*")) {
                        setPort(0);
                    } else {
                        setPort(Integer.parseInt(matcher.group(group + 1)));
                    }

                    // Return a reference to the builder.
                    return this;
                }
            }

            // Not a valid address.
            throw new MalformedAddressException(string);
        }

        /**
         * Build a new ZMQAddress object from the stored host and port.
         *
         * @return a reference to a newly constructed ZMQAddress object.
         */
        @Override
        public ZMQAddress build() {
            return new ZMQAddress(priority, host, port);
        }
    }


    /**
     * Host (IPv4, IPv6, or hostname) on which the addressee resides.
     */
    private final String host;

    /**
     * Port on which the addressee resides.
     */
    private final int port;


    /**
     * Construct a new immutable address with the given parameters.
     *
     * @param priority The priority of the address.
     * @param host The host of the address (IPv4 address, IPv6 address, or hostname).
     * @param port The port of the address.
     */
    protected ZMQAddress(
            @JsonProperty("priority") int priority,
            @JsonProperty("host") String host,
            @JsonProperty("port") int port) {
        super(priority);
        this.host = host;
        this.port = port;
    }

    /**
     * Generate the canonical string representing the address in ZeroMQ endpoints.
     *
     * @return a String object reference.
     */
    @Override
    protected String toAddressString() {
        if (port == 0) {
            return host + ":*";
        }

        return host + ":" + port;
    }
}
