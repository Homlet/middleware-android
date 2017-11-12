package uk.ac.cam.seh208.middleware.demo.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Spinner;

import uk.ac.cam.seh208.middleware.demo.R;
import uk.ac.cam.seh208.middleware.demo.endpoint.Endpoint;


public class EditEndpointActivity extends AppCompatActivity {

    public static final String EXTRA_CNAME = "CNAME";

    private Endpoint endpoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_endpoint);

//        String cname = getIntent().getStringExtra(EXTRA_CNAME);
        String cname = "HELO";

        // Determine whether we are editing an existing endpoint
        // or creating a new one.
        if (!cname.isEmpty()) {
            EditText inputCName = findViewById(R.id.input_cname);
            EditText inputDesc = findViewById(R.id.input_desc);
            Spinner inputPolarity = findViewById(R.id.input_polarity);
            Spinner inputSchema = findViewById(R.id.input_schema);

            // We are editing; populate the inputs with the
            // parameters of the existing endpoint.
//            endpoint = Endpoint.findByCName(cname);
//            inputCName.setText(endpoint.getCName());
//            inputDesc.setText(endpoint.getDesc());
//            inputPolarity.setSelection(/* TODO... */);
//            inputSchema.setSelection(/* TODO... */);

            // If we are editing an existing endpoint, we cannot
            // change its cname. The user must explicitly delete
            // one endpoint and create another to achieve this.
            inputCName.setEnabled(false);
        }
    }
}
