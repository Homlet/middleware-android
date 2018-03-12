package uk.ac.cam.seh208.middleware.demo;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import uk.ac.cam.seh208.middleware.api.Endpoint;
import uk.ac.cam.seh208.middleware.api.exception.MiddlewareDisconnectedException;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;


public class EndpointListAdapter extends RecyclerView.Adapter<EndpointListAdapter.ViewHolder> {

    class ViewHolder extends RecyclerView.ViewHolder {
        View view;

        ViewHolder(View view) {
            super(view);
            this.view = view;
        }
    }


    private final List<Endpoint> endpoints;
    private final EndpointListFragment.OnListItemInteractionListener listener;


    public EndpointListAdapter(List<Endpoint> endpoints,
                               EndpointListFragment.OnListItemInteractionListener listener) {
        this.endpoints = endpoints;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_endpoint, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        try {
            final Endpoint endpoint = endpoints.get(position);
            EndpointDetails details = endpoint.getDetails();

            // Fill in the details in the card.
            TextView name = holder.view.findViewById(R.id.endpoint_name);
            name.setText(details.getName());

            TextView desc = holder.view.findViewById(R.id.endpoint_desc);
            desc.setText(details.getDesc());

            ImageView polarity = holder.view.findViewById(R.id.endpoint_polarity);
            switch (details.getPolarity()) {
                case SOURCE:
                    polarity.setImageResource(R.drawable.ic_endpoint_source_48dp);
                    break;
                case SINK:
                    polarity.setImageResource(R.drawable.ic_endpoint_sink_48dp);
                    break;
            }

            // Set the click handler for the card to signal the activity.
            holder.view.setOnClickListener(v -> {
                if (listener != null) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an endpoint has been selected.
                    listener.onListItemInteraction(endpoint, v);
                }
            });
        } catch (MiddlewareDisconnectedException e) {
            Log.e(MainActivity.getTag(), "Lost connection to middleware in onBindViewHolder.");
        }
    }

    @Override
    public int getItemCount() {
        return endpoints.size();
    }
}
