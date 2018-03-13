package uk.ac.cam.seh208.middleware.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import uk.ac.cam.seh208.middleware.api.Endpoint;
import uk.ac.cam.seh208.middleware.api.Middleware;
import uk.ac.cam.seh208.middleware.api.exception.MiddlewareDisconnectedException;
import uk.ac.cam.seh208.middleware.common.Polarity;


public class CreateEndpointActivity extends AppCompatActivity {

    /**
     * Instance of the middleware interface bound to this activity.
     */
    private Middleware middleware;

    /**
     * Instance of the endpoint interface bound to this activity.
     */
    private Endpoint endpoint;

    @BindView(R.id.input_name)
    EditText inputName;

    @BindView(R.id.input_desc)
    EditText inputDesc;

    @BindView(R.id.input_polarity)
    Spinner inputPolarity;

    @BindView(R.id.input_schema)
    Spinner inputSchema;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_endpoint);
        ButterKnife.bind(this);

        // Connect to the middleware service.
        middleware = new Middleware(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Connect to the middleware service.
        middleware.bind();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Disconnect from the middleware service.
        middleware.unbind();
    }

    @OnClick(R.id.button_save)
    void submit() {
        String name = inputName.getText().toString();
        String desc = inputDesc.getText().toString();
        String schema =
                ResourceUtils.getSchemaFromSpinnerItem(this, inputSchema.getSelectedItem());
        Polarity polarity =
                ResourceUtils.getPolarityFromSpinnerItem(this, inputPolarity.getSelectedItem());

        try {
            switch (polarity) {
                case SOURCE:
                    middleware.createSource(name, desc, schema, null, true, true);
                    break;

                case SINK:
                    middleware.createSink(name, desc, schema, null, true, true);
                    break;
            }
        } catch (MiddlewareDisconnectedException e) {
            Log.e(getTag(), "Lost connection to middleware while creating new endpoint.");
            Toast.makeText(this, R.string.error_contact_middleware, Toast.LENGTH_SHORT).show();
        }

        Toast.makeText(this, R.string.message_endpoint_created, Toast.LENGTH_SHORT).show();
        finish();
    }

    @OnClick(R.id.button_cancel)
    void cancel() {
        finish();
    }

    public static String getTag() {
        return "CREATE";
    }
}
