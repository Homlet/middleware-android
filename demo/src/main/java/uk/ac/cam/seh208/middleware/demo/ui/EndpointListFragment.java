package uk.ac.cam.seh208.middleware.demo.ui;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import uk.ac.cam.seh208.middleware.demo.endpoint.Endpoint;
import uk.ac.cam.seh208.middleware.demo.endpoint.EndpointListAdapter;
import uk.ac.cam.seh208.middleware.demo.R;


public class EndpointListFragment extends Fragment {

    public static final ArrayList<Endpoint> ENDPOINTS = new ArrayList<>();
    static {
        ENDPOINTS.add(new Endpoint(
                "datetime",
                "An endpoint that emits the date-time in ISO-8601 format every second.",
                Endpoint.Polarity.SOURCE
        ));
        ENDPOINTS.add(new Endpoint(
                "app_x",
                "An endpoint that accepts tick signals.",
                Endpoint.Polarity.SINK
        ));
        ENDPOINTS.add(new Endpoint(
                "tick5",
                "An endpoint that emits the character \"1\" every five seconds.",
                Endpoint.Polarity.SOURCE
        ));
        ENDPOINTS.add(new Endpoint(
                "app_y",
                "An endpoint that accepts tick signals.",
                Endpoint.Polarity.SINK
        ));
        ENDPOINTS.add(new Endpoint(
                "tick10",
                "An endpoint that emits the character \"1\" every ten seconds.",
                Endpoint.Polarity.SOURCE
        ));
        ENDPOINTS.add(new Endpoint(
                "tick30",
                "An endpoint that emits the character \"1\" every thirty seconds.",
                Endpoint.Polarity.SOURCE
        ));
    }

    /**
     * Reference to the parent context, for the purposes of signalling events.
     */
    private OnListItemInteractionListener listener;


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
        Context context = view.getContext();
        recycler.setLayoutManager(new LinearLayoutManager(context));
        recycler.setAdapter(new EndpointListAdapter(ENDPOINTS, listener));

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
        final FloatingActionButton button = view.findViewById(R.id.button_add);
        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recycler, int dx, int dy) {
                if (dy < -5 && !button.isShown()) {
                    button.show();
                } else if(dy > 5 && button.isShown()) {
                    button.hide();
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

        if (context instanceof OnListItemInteractionListener) {
            listener = (OnListItemInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListItemInteractionListener");
        }
    }

    /**
     * Forget the previously referenced parent context.
     *
     * Called when the fragment object is detached from a context, such as the main activity.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    /**
     * This interface must be implemented by contexts that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the context, and potentially to sibling fragments.
     */
    public interface OnListItemInteractionListener {
        void onListItemInteraction(Endpoint endpoint, View view);
    }
}
