package uk.ac.cam.seh208.middleware.core.network.impl;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.cam.seh208.middleware.core.exception.MalformedAddressException;
import uk.ac.cam.seh208.middleware.core.network.Address;
import uk.ac.cam.seh208.middleware.core.network.AddressBuilder;


/**
 * Implementation of a ZeroMQ address.
 */
public class ZMQAddress extends Address {

    /**
     * Return the public IP of the local host.
     *
     * @return a String formatted IP address.
     *
     * @throws UnknownHostException if the host has no bound IP address.
     */
    public static String getLocalHost() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress();
    }


    public static class Builder implements AddressBuilder {

        private String host;

        private int port;


        public Builder() {
            host = "*";
            port = 0;
        }

        public Builder setHost(String host) {
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
            String wildcardExpr = "\\*:(\\d+|\\*)";

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
                    host = matcher.group(group);
                    if (matcher.group(group + 1).equals("*")) {
                        port = 0;
                    } else {
                        port = Integer.parseInt(matcher.group(group + 1));
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
            return new ZMQAddress(host, port);
        }
    }


    /**
     * Host (IPv4, IPv6, or hostname) on which the addressee resides.
     */
    public final String host;

    /**
     * Port on which the addressee resides.
     */
    public final int port;


    /**
     * Construct a new immutable address with the given parameters.
     *
     * @param host The host of the address (IPv4 address, IPv6 address, or hostname).
     * @param port The port of the address.
     */
    protected ZMQAddress(
            @JsonProperty("host") String host,
            @JsonProperty("port") int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Generate the canonical string representing the address in ZeroMQ endpoints.
     *
     * @return a String object reference.
     */
    @Override
    public String toCanonicalString() {
        if (port == 0) {
            return host + ":*";
        }

        return host + ":" + port;
    }
}
