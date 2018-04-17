package uk.ac.cam.seh208.middleware.core.comms;

import android.util.Log;

import java.net.SocketException;
import java.util.HashMap;
import java.util.List;

import uk.ac.cam.seh208.middleware.core.comms.impl.ZMQMessageContext;
import uk.ac.cam.seh208.middleware.core.comms.impl.ZMQSchemeConfiguration;


/**
 * Switchboard class aggregating many network implementations and switching between
 * them based on address type. This switch is specialised for message streams.
 */
public class MessageSwitch implements Environment {

    /**
     * Enumeration of local interface addresses on which this environment
     * is accessible.
     */
    private Location location;

    /**
     * Store of message contexts by scheme string.
     */
    private HashMap<String, MessageContext> contextsByScheme;


    /**
     * Construct and initialise message and request contexts for the given schemes.
     */
    public MessageSwitch(List<SchemeConfiguration> configurations) {
        contextsByScheme = new HashMap<>();
        location = new Location();

        // Create contexts for the requested schemes.
        for (SchemeConfiguration configuration : configurations) {
            String scheme = configuration.getScheme();
            switch (scheme) {
                case Address.SCHEME_ZMQ:
                    try {
                        MessageContext context = new ZMQMessageContext(
                                this, (ZMQSchemeConfiguration) configuration);
                        location.addAddresses(context.getInterfaceAddresses());
                        contextsByScheme.put(scheme, context);
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                    break;

                // case SCHEME_BLUETOOTH etc...

                default:
                    Log.w(getTag(), "Unknown context scheme given in constructor.");
                    break;
            }
        }
    }

    /**
     * Defer to the relevant message context to create a stream to the given address.
     *
     * NOTE: the message context defines caching policy for existing streams.
     *
     * @param address Address of the remote host to which to get a stream.
     *
     * @return a stream to the given address. Whether this stream is newly constructed
     *         depends on the message context implementation.
     */
    public MessageStream getStream(Address address) {
        String scheme = Address.getSchemeString(address);
        return contextsByScheme.get(scheme).getMessageStream(address);
    }

    /**
     * @return an enumeration of the local interface addresses on which this
     *         environment is accessible.
     */
    public Location getLocation() {
        return location;
    }

    private static String getTag() {
        return "MSG_SWITCH";
    }
}
