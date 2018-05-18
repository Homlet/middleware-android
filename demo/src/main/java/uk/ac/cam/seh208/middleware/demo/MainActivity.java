package uk.ac.cam.seh208.middleware.demo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import uk.ac.cam.seh208.middleware.api.Middleware;
import uk.ac.cam.seh208.middleware.api.RDC;
import uk.ac.cam.seh208.middleware.api.MiddlewareDisconnectedException;
import uk.ac.cam.seh208.middleware.metrics.Metrics;
import uk.ac.cam.seh208.middleware.metrics.MetricsClient;
import uk.ac.cam.seh208.middleware.metrics.MiddlewareClient;
import uk.ac.cam.seh208.middleware.metrics.MiddlewareServer;
import uk.ac.cam.seh208.middleware.metrics.TCPClient;
import uk.ac.cam.seh208.middleware.metrics.TCPServer;
import uk.ac.cam.seh208.middleware.metrics.ZMQClient;
import uk.ac.cam.seh208.middleware.metrics.ZMQServer;
import uk.ac.cam.seh208.middleware.metrics.IncompleteMetricsException;

import static android.os.Environment.DIRECTORY_DOCUMENTS;


public class MainActivity extends AppCompatActivity {

    /**
     * Subdirectory of the public documents directory where metrics are stored.
     */
    private static final String DIRECTORY_METRICS = "mw_metrics";

    /**
     * Key used to detect when the storage permission has just been granted.
     */
    private static final int PERMISSION_STORAGE = 0;

    /**
     * Key used to store the preference for the RDC to disk.
     */
    private static final String PREFS_RUN_RDC = "RUN_RDC";

    /**
     * Key used to store the RDC address to disk.
     */
    private static final String PREFS_RDC_ADDR = "RDC_ADDR";


    /**
     * Instance of the middleware interface bound to this activity.
     */
    private Middleware middleware;

    /**
     * Instance of the middleware metrics server which runs on this activity.
     */
    private MiddlewareServer middlewareServer;

    /**
     * Instance of the ZeroMQ metrics server which runs on this activity.
     */
    private ZMQServer zmqServer;

    /**
     * Instance of the TCP/IP metrics server which runs on this activity.
     */
    private TCPServer tcpServer;

    /**
     * Reference to the bottom navigation bar view.
     */
    @BindView(R.id.navigation)
    BottomNavigationView navigation;

    /**
     * Callback for when an item on the bottom navigation bar is selected.
     *
     * Call the navigation routine to load the relevant fragment into the interface.
     */
    private BottomNavigationView.OnNavigationItemSelectedListener listener =
            (MenuItem item) -> navigateTo(item.getItemId(), false);


    /**
     * Inflate the default 'triple dot' options_main menu on the action bar.
     *
     * @param menu Reference to the menu object.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_main, menu);

        return true;
    }

    /**
     * Called when the options menu is opened by the user.
     *
     * @param menu Reference to the menu object.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // Get the current value of the RDC checkbox, and set it in the menu item.
        boolean running = getSharedPreferences("preferences", 0).getBoolean(PREFS_RUN_RDC, false);
        menu.findItem(R.id.action_run_rdc).setChecked(running);

        return true;
    }

    /**
     * Called when a menu item is interacted with by the user.
     *
     * @param item Reference to the menu item that was selected.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_run_rdc:
                boolean running = !item.isChecked();

                // Check or un-check the checkbox.
                item.setChecked(running);

                // Store the preference persistently.
                getSharedPreferences("preferences", 0)
                        .edit()
                        .putBoolean(PREFS_RUN_RDC, running)
                        .apply();

                // Start or stop the RDC.
                if (running) {
                    RDC.start(this);
                } else {
                    RDC.stop(this);
                }

                return true;

            case R.id.action_set_rdc_address:
                // Get the old stored address.
                final SharedPreferences preferences = getSharedPreferences("preferences", 0);
                String oldAddress = preferences.getString(
                        PREFS_RDC_ADDR, getString(R.string.default_rdc_addr));

                // Set up the text input.
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setText(oldAddress);

                // Show a dialog for setting the address.
                new AlertDialog.Builder(this)
                        .setTitle(R.string.action_set_rdc_address)
                        .setView(input)
                        .setPositiveButton("Set", (dialog, which) -> {
                            // Set the new address in the preferences.
                            String newAddress = input.getText().toString();
                            preferences.edit()
                                    .putString(PREFS_RDC_ADDR, newAddress)
                                    .apply();

                            try {
                                // Set the new address in the middleware.
                                middleware.setRDCAddress(newAddress);
                            } catch (MiddlewareDisconnectedException e) {
                                Log.e(getTag(), "Middleware disconnected setting RDC address.");
                                Toast.makeText(
                                        this,
                                        R.string.error_contact_middleware,
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            }

                            // Dismiss the dialog.
                            dialog.dismiss();
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                        .show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Inflate the user interface with views, and configure these before displaying.
     *
     * Called on creation of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Configure the bottom navigation bar.
        navigation.setOnNavigationItemSelectedListener(listener);

        // Load the endpoints page.
        navigateTo(R.id.page_endpoints);

        // Instantiate the middleware interface.
        middleware = new Middleware(this);

        // Create the metrics servers.
        middlewareServer = new MiddlewareServer(middleware);
        zmqServer = new ZMQServer();
        tcpServer = new TCPServer();

        // Get the current value of the RDC checkbox, and start the RDC if necessary.
        boolean running = getSharedPreferences("preferences", 0).getBoolean(PREFS_RUN_RDC, false);
        if (running) {
            RDC.start(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Connect to the middleware service.
        middleware.bind(this::onMiddlewareBind);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Stop the metrics servers (if running).
        middlewareServer.stop();
        zmqServer.stop();
        tcpServer.stop();

        // Disconnect from the middleware service.
        middleware.unbind();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_STORAGE:
                if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO: flush buffered files.
                }
                break;
        }
    }

    private void onMiddlewareBind() {
        try {
            // Get the old stored address and set it in the middleware.
            final SharedPreferences preferences = getSharedPreferences("preferences", 0);
            String oldAddress = preferences.getString(
                    PREFS_RDC_ADDR, getString(R.string.default_rdc_addr));
            middleware.setRDCAddress(oldAddress);
        } catch (MiddlewareDisconnectedException e) {
            Log.e(getTag(), "Middleware disconnected while configuring.");
            Toast.makeText(this, R.string.error_contact_middleware, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    public Middleware getMiddleware() {
        return middleware;
    }

    /**
     * Call the navigation routine, updating the selected item on the navigation bar.
     *
     * @see #navigateTo(int, boolean)
     */
    private void navigateTo(@IdRes int page) {
        navigateTo(page, true);
    }

    /**
     * Load the fragment associated with a given page into the interface.
     *
     * @param page             Resource ID of the page to navigate to.
     * @param updateNavigation If true, the navigation bar will be updated to reflect the change.
     *
     * @return whether navigation was successful.
     */
    private boolean navigateTo(@IdRes int page, boolean updateNavigation) {
        // Find the navigation menu item associated with the page.
        MenuItem item = navigation.getMenu().findItem(page);
        if (item == null) {
            // If the page is not represented in the navigation menu, we can't navigate to it.
            return false;
        }

        // Replace the page fragment with a new instance.
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.page_container, newPageFragment(page), getPageTitle(page));
        transaction.commit();

        if (updateNavigation) {
            // Update the highlighted tab on the bottom navigation bar.
            item.setChecked(true);
        }

        return true;
    }

    /**
     * Construct a new fragment object associated with a particular page.
     *
     * @param page Resource ID of the page.
     *
     * @return a newly constructed fragment object reference.
     */
    private Fragment newPageFragment(@IdRes int page) {
        switch(page) {
            case R.id.page_endpoints:
                return new EndpointListFragment();
            case R.id.page_remote:
                return new MetricsFragment();
            default:
                return null;
        }
    }

    /**
     * Get the title of a given page.
     *
     * @param page Resource ID of the page.
     *
     * @return the string title of the given page.
     */
    private String getPageTitle(@IdRes int page) {
        switch(page) {
            case R.id.page_endpoints:
                return getString(R.string.title_endpoints);
            case R.id.page_remote:
                return getString(R.string.title_metrics);
            default:
                return null;
        }
    }

    public MiddlewareServer getMiddlewareServer() {
        return middlewareServer;
    }

    public ZMQServer getZMQServer() {
        return zmqServer;
    }

    public TCPServer getTCPServer() {
        return tcpServer;
    }

    private static class MetricsTask extends AsyncTask<Object, Void, Metrics> {

        private String typename;

        private WeakReference<MainActivity> activity;


        @Override
        protected Metrics doInBackground(Object... objects) {
            // Unpack the closure arguments.
            MetricsClient client = (MetricsClient) objects[0];
            int messages = (Integer) objects[1];
            int length = (Integer) objects[2];
            //noinspection unchecked
            activity = (WeakReference<MainActivity>) objects[3];

            typename = client.getName();

            try {
                // Run metrics on the client.
                return client.runMetrics(messages, length);
            } catch (IncompleteMetricsException e) {
                Log.w(getTag(), "Metrics were incomplete; something went wrong gathering.");
                return null;
            }
        }

        @Override
        protected void onPostExecute(Metrics metrics) {
            if (metrics == null) {
                return;
            }

            MainActivity activity = this.activity.get();
            if (activity == null) {
                return;
            }

            // Generate a unique filename for the metrics data.
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.UK);
            String filename = typename +
                    "_" + formatter.format(new Date()) +
                    "_" + metrics.getMessages() + "msg" +
                    "_" + metrics.getMessageLength() + "len.csv";

            // Save the metrics data to CSV file.
            activity.writeFile(DIRECTORY_METRICS, filename, metrics.toCommaSeparatedValues());

            // Show a summary alert to the user.
            new AlertDialog.Builder(activity)
                    .setTitle("Metrics (mean | median)")
                    .setMessage("Latency: " + (int) metrics.getMeanLatency() +
                                    " \u00B5s | " + (int) metrics.getMedianLatency() +
                                    " \u00B5s\n" +
                                "Throughput: " + (int) metrics.getMeanReceivingThroughput() +
                                    " msg/s | " + (int) metrics.getMedianReceivingThroughput() +
                                    " msg/s\n\n" +
                                "Saved to \"" + filename + "\"")
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    public void runMiddlewareMetrics(int messages, int length) {
        if (middleware == null) {
            Log.w(getTag(), "Tried to run middleware metrics while middleware unbound.");
            return;
        }

        MetricsTask task = new MetricsTask();
        task.execute(
                new MiddlewareClient(middleware),
                messages,
                length,
                new WeakReference<>(this));
    }

    public void runZMQMetrics(final int messages, final int length) {
        // Set up the text input.
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(R.string.default_zmq_host);

        // Show a dialog to get the desired server address.
        new AlertDialog.Builder(this)
                .setTitle(R.string.run_zeromq_metrics)
                .setView(input)
                .setPositiveButton("Run", (dialog, which) -> {
                    // Run the metrics task with the given host.
                    MetricsTask task = new MetricsTask();
                    task.execute(
                            new ZMQClient(input.getText().toString()),
                            messages,
                            length,
                            new WeakReference<>(MainActivity.this)
                    );

                    // Dismiss the dialog.
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }

    public void runTCPMetrics(final int messages, final int length) {
        // Set up the text input.
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(R.string.default_tcp_host);

        // Show a dialog to get the desired server address.
        new AlertDialog.Builder(this)
                .setTitle(R.string.run_tcp_ip_metrics)
                .setView(input)
                .setPositiveButton("Run", (dialog, which) -> {
                    // Run the metrics task with the given host.
                    MetricsTask task = new MetricsTask();
                    task.execute(
                            new TCPClient(input.getText().toString()),
                            messages,
                            length,
                            new WeakReference<>(MainActivity.this)
                    );

                    // Dismiss the dialog.
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }

    public void writeFile(String directory, String filename, String contents) {
        if (getStoragePermission()) {
            // Create the parent directory for the file.
            File dir = new File(
                    Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS),
                    directory);
            if (!dir.exists() && !dir.mkdirs()) {
                Log.w(getTag(), "Could not create directory " + dir.getAbsolutePath());
                return;
            }

            // Write the data to the file.
            File file = new File(dir, filename);
            try (FileWriter writer = new FileWriter(file)) {
                writer.append(contents);
            } catch (FileNotFoundException e) {
                Log.w(getTag(), "Could not create file " + file.getAbsolutePath());
            } catch (IOException e) {
                Log.w(getTag(), "Error writing to file", e);
            }
        } else {
            // TODO: buffer file for writing later.
        }
    }

    public boolean getStoragePermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    PERMISSION_STORAGE);

            return false;
        }

        return true;
    }

    public static String getTag() {
        return "MAIN";
    }
}
