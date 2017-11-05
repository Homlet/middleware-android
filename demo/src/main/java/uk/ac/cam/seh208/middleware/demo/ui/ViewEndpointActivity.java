package uk.ac.cam.seh208.middleware.demo.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import uk.ac.cam.seh208.middleware.demo.R;


public class ViewEndpointActivity extends AppCompatActivity {

    /**
     * TODO: document.
     */
    public static final String EXTRA_CNAME = "CNAME";

    /**
     * TODO: document.
     */
    private String cname;


    /**
     * TODO: document.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_endpoint);

        // Extract the cname from the passed bundle.
        cname = getIntent().getStringExtra(EXTRA_CNAME);
    }


    /**
     * Inflate the action bar menu with an edit button.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_view_endpoint, menu);
        return true;
    }

    /**
     * TODO: document.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit:
                // The user wants to edit the endpoint details.
                Intent intent = new Intent(this, EditEndpointActivity.class);
                intent.putExtra(ViewEndpointActivity.EXTRA_CNAME, cname);
                startActivity(intent);
                return true;

            case android.R.id.home:
                // Invoke 'back' instead of 'up' to use the nice transition animation.
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
