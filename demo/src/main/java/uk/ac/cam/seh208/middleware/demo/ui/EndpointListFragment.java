package uk.ac.cam.seh208.middleware.demo.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import uk.ac.cam.seh208.middleware.demo.endpoint.EndpointAdapter;
import uk.ac.cam.seh208.middleware.demo.R;
import uk.ac.cam.seh208.middleware.demo.endpoint.dummy.DummyContent;
import uk.ac.cam.seh208.middleware.demo.endpoint.dummy.DummyContent.DummyItem;


/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class EndpointListFragment extends Fragment {

    /**
     * Reference to the parent context, for the purposes of signalling events.
     */
    private OnListFragmentInteractionListener listener;


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
        if (recycler != null) {
            Context context = view.getContext();
            recycler.setLayoutManager(new LinearLayoutManager(context));
            recycler.setAdapter(new EndpointAdapter(DummyContent.ITEMS, listener));
        }

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

        if (context instanceof OnListFragmentInteractionListener) {
            listener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
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
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(DummyItem item);
    }
}
