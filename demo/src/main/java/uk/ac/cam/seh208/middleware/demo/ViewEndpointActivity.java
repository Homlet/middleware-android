package uk.ac.cam.seh208.middleware.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;
import uk.ac.cam.seh208.middleware.api.Endpoint;
import uk.ac.cam.seh208.middleware.api.Middleware;
import uk.ac.cam.seh208.middleware.api.MiddlewareDisconnectedException;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;


public class ViewEndpointActivity extends AppCompatActivity {

    /**
     * Key for the intent extra storing the name of the endpoint we are viewing.
     */
    public static final String EXTRA_NAME = "NAME";


    /**
     * Instance of the middleware interface bound to this activity.
     */
    private Middleware middleware;

    /**
     * Name of the endpoint bound to this activity.
     */
    private String name;

    @BindView(R.id.endpoint_name)
    TextView textName;

    @BindView(R.id.endpoint_desc)
    TextView textDesc;

    @BindView(R.id.endpoint_polarity)
    ImageView imagePolarity;

    @BindView(R.id.endpoint_schema)
    TextView textSchema;

    @BindView(R.id.endpoint_tags)
    TextView textTags;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_endpoint);
        ButterKnife.bind(this);

        // Instantiate the middleware interface.
        middleware = new Middleware(this);

        // Extract the name from the passed bundle.
        name = getIntent().getStringExtra(EXTRA_NAME);

        Log.i(getTag(), "Created view endpoint activity for endpoint \"" + name + "\"");
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Connect to the middleware service.
        middleware.bind(this::onMiddleware);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Disconnect from the middleware service.
        middleware.unbind();
    }

    private void onMiddleware() {
        // Get a reference to the endpoint-specific interface for this endpoint.
        Endpoint endpoint = middleware.getEndpoint(name);

        try {
            // Get the endpoint details.
            EndpointDetails details = endpoint.getDetails();

            // Populate the UI with endpoint properties.
            textName.setText(details.getName());
            textDesc.setText(details.getDesc());
            imagePolarity.setImageResource(
                    ResourceUtils.getPolarityImageResource(details.getPolarity()));
            textSchema.setText(details.getSchema());
            textTags.setText(StreamSupport
                    .stream(details.getTags())
                    .collect(Collectors.joining(", ")));
        } catch (MiddlewareDisconnectedException e) {
            Log.e(getTag(), "Middleware disconnected while getting endpoint details.");
            Toast.makeText(this, R.string.error_contact_middleware, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Override the default behaviour on up navigation to preserve card animations.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Invoke 'back' instead of 'up' to use the nice transition animation.
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static String getTag() {
        return "VIEW";
    }
}
