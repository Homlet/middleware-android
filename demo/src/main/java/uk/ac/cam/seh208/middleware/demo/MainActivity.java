package uk.ac.cam.seh208.middleware.demo;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import butterknife.ButterKnife;
import uk.ac.cam.seh208.middleware.api.Middleware;
import uk.ac.cam.seh208.middleware.api.RDC;


public class MainActivity extends AppCompatActivity {

    /**
     * Instance of the middleware interface bound to this activity.
     */
    private Middleware middleware;

    /**
     * Reference to the bottom navigation bar view.
     */
    private BottomNavigationView navigation;

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
        navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(listener);

        // Load the endpoints page.
        navigateTo(R.id.page_endpoints);

        // Instantiate the middleware interface.
        middleware = new Middleware(this);

        // Start the RDC if not started already.
        RDC.start(this);
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
                return new Fragment();  // TODO: create fragment for remote page.
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
                return getString(R.string.title_remote);
            default:
                return null;
        }
    }

    public static String getTag() {
        return "MAIN";
    }
}
