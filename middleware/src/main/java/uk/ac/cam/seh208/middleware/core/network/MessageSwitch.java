package uk.ac.cam.seh208.middleware.core.network;

import android.util.Log;

import java.util.HashMap;
import java.util.List;

import uk.ac.cam.seh208.middleware.core.network.impl.ZMQMessageContext;
import uk.ac.cam.seh208.middleware.core.network.impl.ZMQSchemeConfiguration;


/**
 * Switchboard class aggregating many network implementations and switching between
 * them based on address type. This switch is specialised for message streams.
 */
public class MessageSwitch {

    /**
     * Store of message contexts by scheme string.
     */
    private HashMap<String, MessageContext> contextsByScheme;


    /**
     * Construct and initialise message and request contexts for the given schemes.
     */
    public MessageSwitch(List<SchemeConfiguration> configurations) {
        contextsByScheme = new HashMap<>();

        // Create contexts for the requested schemes.
        for (SchemeConfiguration configuration : configurations) {
            String scheme = configuration.getScheme();
            switch (scheme) {
                case Address.SCHEME_ZMQ:
                    MessageContext context = new ZMQMessageContext(
                            (ZMQSchemeConfiguration) configuration);
                    contextsByScheme.put(scheme, context);
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
        String scheme = Address.getScheme(address);
        return contextsByScheme.get(scheme).getMessageStream(address);
    }


    private static String getTag() {
        return "MSG_SWITCH";
    }
}
