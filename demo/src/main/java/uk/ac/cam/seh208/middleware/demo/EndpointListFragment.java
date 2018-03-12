package uk.ac.cam.seh208.middleware.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;

import uk.ac.cam.seh208.middleware.api.Endpoint;
import uk.ac.cam.seh208.middleware.api.exception.MiddlewareDisconnectedException;


public class EndpointListFragment extends Fragment {

    private final ArrayList<Endpoint> endpoints = new ArrayList<>();

    /**
     * Reference to the parent context, for the purposes of signalling events.
     */
    private Context context;


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

        // Set the adapter for the recycler view.
        RecyclerView recycler = view.findViewById(R.id.endpoint_list);
        recycler.setLayoutManager(new LinearLayoutManager(context));
        recycler.setAdapter(new EndpointListAdapter(endpoints, (e, v) -> {
            try {
                Intent intent = new Intent(context, ViewEndpointActivity.class);
                intent.putExtra(ViewEndpointActivity.EXTRA_NAME, e.getDetails().getName());
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        (Activity) context, v, "view_endpoint_container"
                );
                context.startActivity(intent, options.toBundle());
            } catch (MiddlewareDisconnectedException ex) {
                Toast.makeText(context, R.string.error_contact_middleware, Toast.LENGTH_SHORT).show();
            }
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

        // Configure the floating action button (FAB) for creating new endpoints.
        final FloatingActionButton plusButton = view.findViewById(R.id.button_add);
        plusButton.setOnClickListener(v -> {
            // When the button is pressed, start a new activity for configuring the endpoint.
            Intent intent = new Intent(context, CreateEndpointActivity.class);
            context.startActivity(intent);
        });
        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recycler, int dx, int dy) {
                // Hide the floating action button on scrolling the list.
                if (dy < -5 && !plusButton.isShown()) {
                    plusButton.show();
                } else if(dy > 5 && plusButton.isShown()) {
                    plusButton.hide();
                }
            }
        });

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

        this.context = context;
    }

    /**
     * Forget the previously referenced parent context.
     *
     * Called when the fragment object is detached from a context, such as the main activity.
     */
    @Override
    public void onDetach() {
        super.onDetach();

        context = null;
    }

    public interface OnListItemInteractionListener {
        void onListItemInteraction(Endpoint endpoint, View view);
    }
}
