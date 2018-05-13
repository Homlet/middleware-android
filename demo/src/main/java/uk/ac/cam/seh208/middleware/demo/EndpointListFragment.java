package uk.ac.cam.seh208.middleware.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import uk.ac.cam.seh208.middleware.api.Middleware;
import uk.ac.cam.seh208.middleware.api.exception.MiddlewareDisconnectedException;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;


public class EndpointListFragment extends Fragment {

    public interface OnListItemInteractionListener {
        void onListItemInteraction(EndpointDetails details, View view);
    }

    private static class UpdateEndpointDetailsTask
            extends AsyncTask<UpdateEndpointDetailsTask.Args, Void, Void> {

        static class Args {
            Args(List<EndpointDetails> details, Middleware middleware,
                 RecyclerView.Adapter adapter, SwipeRefreshLayout refresher,
                 Activity activity) {
                this.details = details;
                this.middleware = middleware;
                this.adapter = adapter;
                this.refresher = refresher;
                this.activity = activity;
            }

            final List<EndpointDetails> details;
            final Middleware middleware;
            final RecyclerView.Adapter adapter;
            final SwipeRefreshLayout refresher;
            final Activity activity;
        }

        @Override
        protected Void doInBackground(Args... args) {
            Log.i(MainActivity.getTag(), "Updating endpoint details list...");

            try {
                // Get an updated endpoint details list.
                List<EndpointDetails> newDetails = args[0].middleware.getAllEndpointDetails();

                synchronized (args[0].details) {
                    // Clear the old list.
                    args[0].details.clear();

                    // Populate the old list with the new details;
                    args[0].details.addAll(newDetails);
                }

                Log.i(MainActivity.getTag(), "Found " + newDetails.size() + " endpoints.");

                args[0].activity.runOnUiThread(() -> {
                    args[0].refresher.setRefreshing(false);
                    args[0].adapter.notifyDataSetChanged();
                });
            } catch (MiddlewareDisconnectedException e) {
                Log.e(MainActivity.getTag(), "Error updating endpoint details list.");

                args[0].activity.runOnUiThread(() -> {
                    args[0].refresher.setRefreshing(false);
                    Toast.makeText(
                            args[0].activity,
                            R.string.error_contact_middleware,
                            Toast.LENGTH_SHORT).show();
                });
            }

            return null;
        }
    }


    /**
     * Mutable endpoint details list tracking the current active endpoints in
     * the middleware instance.
     */
    private final List<EndpointDetails> details = new ArrayList<>();

    /**
     * Reference to the parent context, for the purposes of signalling events.
     */
    private Context context;

    private Middleware middleware;

    @BindView(R.id.endpoint_list_swipe)
    SwipeRefreshLayout refresher;

    @BindView(R.id.endpoint_list)
    RecyclerView recycler;

    @BindView(R.id.button_add)
    FloatingActionButton buttonAdd;


    /**
     * Create a view and inflate it with a card list and action button.
     *
     * Called when the fragment object is required to create a view, such as when
     * it is first displayed after being attached to the main activity.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_endpoints, container, false);
        ButterKnife.bind(this, view);

        // Set the refresh listener for the swipe refresh layout.
        refresher.setOnRefreshListener(() -> {
            refresher.setRefreshing(true);
            updateEndpointDetails();
        });

        // Set the adapter for the recycler view.
        recycler.setLayoutManager(new LinearLayoutManager(context));
        recycler.setAdapter(new EndpointListAdapter(details, (d, v) -> {
            Intent intent = new Intent(context, ViewEndpointActivity.class);
            intent.putExtra(ViewEndpointActivity.EXTRA_NAME, d.getName());
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    (Activity) context, v, "view_endpoint_container"
            );
            context.startActivity(intent, options.toBundle());
        }));

        // Add padding before the first card in the recycler.
        recycler.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                       RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);

                if (parent.getChildAdapterPosition(view) == 0) {
                    outRect.top += getResources().getDimensionPixelOffset(R.dimen.card_margin);
                }
            }
        });

        // Hide the floating action button on scrolling the list.
        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recycler, int dx, int dy) {
                if (dy < -5 && !buttonAdd.isShown()) {
                    buttonAdd.show();
                } else if(dy > 5 && buttonAdd.isShown()) {
                    buttonAdd.hide();
                }
            }
        });

        // Use an ItemTouchHelper to implement swipe-to-destroy endpoints.
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT |
                                                      ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                // If the view holder position is an invalid list index, return.
                int position = ((EndpointListAdapter.ViewHolder) viewHolder).currentPosition;
                if (position < 0 && details.size() <= position) {
                    return;
                }

                try {
                    // Try to destroy the endpoint in the middleware.
                    // TODO: move this into a background task.
                    EndpointDetails endpoint = details.get(position);
                    middleware.destroyEndpoint(endpoint.getName());
                } catch (MiddlewareDisconnectedException e) {
                    Log.e(getTag(), "Middleware disconnected destroying endpoint.");
                }

                // Remove the endpoint from the local list and notify the recycler.
                details.remove(position);
                recycler.getAdapter().notifyDataSetChanged();
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        itemTouchHelper.attachToRecyclerView(recycler);

        // Update the endpoints list.
        updateEndpointDetails();

        return view;
    }

    /**
     * Store a reference to the parent context, in order to signal events to it.
     *
     * Called when the fragment object is attached to a context, such as the main activity.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Keep a reference to the attached context.
        this.context = context;

        // Get a reference to the middleware from the attached context.
        middleware = ((MainActivity) context).getMiddleware();
    }

    /**
     * Called when the main activity comes back into focus.
     */
    @Override
    public void onResume() {
        super.onResume();

        updateEndpointDetails();
    }

    /**
     * Forget the previously referenced parent context.
     *
     * Called when the fragment object is detached from a context, such as the main activity.
     */
    @Override
    public void onDetach() {
        super.onDetach();

        // Drop the reference to the context.
        context = null;
    }

    /**
     * When the add button is pressed, start a new activity for creating the new endpoint.
     */
    @OnClick(R.id.button_add)
    void add(View view) {
        Intent intent = new Intent(context, CreateEndpointActivity.class);
        context.startActivity(intent);
    }

    private void updateEndpointDetails() {
        // Pack the task arguments.
        UpdateEndpointDetailsTask.Args args = new UpdateEndpointDetailsTask.Args(
                details, middleware, recycler.getAdapter(), refresher, getActivity());

        // Execute the update task.
        new UpdateEndpointDetailsTask().execute(args);
    }
}
