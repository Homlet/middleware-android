package uk.ac.cam.seh208.middleware.demo.endpoint;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import uk.ac.cam.seh208.middleware.demo.R;
import uk.ac.cam.seh208.middleware.demo.ui.EndpointListFragment.OnListFragmentInteractionListener;
import uk.ac.cam.seh208.middleware.demo.endpoint.dummy.DummyContent.DummyItem;

import java.util.List;


public class EndpointAdapter extends RecyclerView.Adapter<EndpointAdapter.ViewHolder> {

    private final List<DummyItem> mValues;
    private final OnListFragmentInteractionListener mListener;


    public EndpointAdapter(List<DummyItem> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_endpoint, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);

        holder.mView.setOnClickListener(v -> {
            if (null != mListener) {
                // Notify the active callbacks interface (the activity, if the
                // fragment is attached to one) that an item has been selected.
                mListener.onListFragmentInteraction(holder.mItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        DummyItem mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
        }
    }
}
