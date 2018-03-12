package uk.ac.cam.seh208.middleware.demo;

import android.content.Context;

import java.util.Objects;

import uk.ac.cam.seh208.middleware.common.Polarity;

class ResourceUtils {
    static int getPolarityImageResource(Polarity polarity) {
        switch (polarity) {
            case SOURCE:
                return R.drawable.ic_endpoint_source_48dp;

            case SINK:
                return R.drawable.ic_endpoint_sink_48dp;

            default:
                return -1;
        }
    }

    static Polarity getPolarityFromSpinnerItem(Context context, Object item) {
        if (!(item instanceof String)) {
            return null;
        }

        String string = (String) item;

        if (Objects.equals(string, context.getString(R.string.polarity_source))) {
            return Polarity.SOURCE;
        }

        if (Objects.equals(string, context.getString(R.string.polarity_sink))) {
            return Polarity.SINK;
        }

        return null;
    }

    static String getSchemaFromSpinnerItem(Context context, Object item) {
        if (!(item instanceof String)) {
            return null;
        }

        String string = (String) item;

        if (Objects.equals(string, context.getString(R.string.schema_text))) {
            return "{\"type\": \"string\"}";
        }

        if (Objects.equals(string, context.getString(R.string.schema_int))) {
            return "{\"type\": \"integer\"}";
        }

        return null;
    }
}
