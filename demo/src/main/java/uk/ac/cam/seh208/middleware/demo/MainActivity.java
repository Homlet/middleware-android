package uk.ac.cam.seh208.middleware.demo;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import uk.ac.cam.seh208.middleware.api.Middleware;
import uk.ac.cam.seh208.middleware.api.RDC;
import uk.ac.cam.seh208.middleware.api.exception.MiddlewareDisconnectedException;
import uk.ac.cam.seh208.middleware.metrics.Metrics;
import uk.ac.cam.seh208.middleware.metrics.MetricsClient;
import uk.ac.cam.seh208.middleware.metrics.MiddlewareClient;
import uk.ac.cam.seh208.middleware.metrics.MiddlewareServer;
import uk.ac.cam.seh208.middleware.metrics.TCPServer;
import uk.ac.cam.seh208.middleware.metrics.ZMQServer;
import uk.ac.cam.seh208.middleware.metrics.exception.IncompleteMetricsException;


public class MainActivity extends AppCompatActivity {


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
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_main, menu);
        return true;
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
        zmqServer = new ZMQServer(this);
        tcpServer = new TCPServer(this);

        // Start the RDC if not started already.
        RDC.start(this);
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

    private void onMiddlewareBind() {
        try {
            middleware.setRDCAddress("zmq://127.0.0.1:4854");
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
    private boolean navigateTo(@IdRes int page) {
        return navigateTo(page, true);
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
            case R.id.page_resources:
                return new Fragment();  // TODO: create fragment for resources page.
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
            case R.id.page_resources:
                return getString(R.string.title_resources);
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

        private WeakReference<Context> context;


        @Override
        protected Metrics doInBackground(Object... objects) {
            // Unpack the closure arguments.
            MetricsClient client = (MetricsClient) objects[0];
            int messages = (Integer) objects[1];
            int delayMillis = (Integer) objects[2];
            context = (WeakReference<Context>) objects[3];

            // Run metrics on the client.
            client.connect();
            client.runLatency(messages, delayMillis);
            client.runThroughput(messages);
            client.disconnect();

            try {
                return client.getMetrics();
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

            Context context = this.context.get();
            if (context == null) {
                return;
            }

            new AlertDialog.Builder(context)
                    .setTitle("Metrics")
                    .setMessage("Latency: " + (int) metrics.latency + " \u00B5s\n" +
                                "Throughput:" + (int) metrics.throughput + " messages per second")
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    public void runMiddlewareMetrics(int messages, int delayMillis) {
        if (middleware == null) {
            Log.w(getTag(), "Tried to run middleware metrics while middleware unbound.");
            return;
        }

        MetricsTask task = new MetricsTask();
        task.execute(
                new MiddlewareClient(middleware),
                messages,
                delayMillis,
                new WeakReference<>(this));
    }

    public static String getTag() {
        return "MAIN";
    }
}
