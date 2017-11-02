package uk.ac.cam.seh208.middleware.demo.endpoint;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import uk.ac.cam.seh208.middleware.demo.R;
import uk.ac.cam.seh208.middleware.demo.ui.EndpointListFragment;

import java.util.List;


public class EndpointAdapter extends RecyclerView.Adapter<EndpointAdapter.ViewHolder> {

    private final List<Endpoint> endpoints;
    private final EndpointListFragment.OnListItemInteractionListener listener;


    public EndpointAdapter(List<Endpoint> endpoints,
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
        final Endpoint endpoint = endpoints.get(position);

        // Fill in the details in the card.
        TextView cname = holder.view.findViewById(R.id.endpoint_cname);
        cname.setText(endpoint.getCName());

        TextView desc = holder.view.findViewById(R.id.endpoint_desc);
        desc.setText(endpoint.getDesc());

        ImageView polarity = holder.view.findViewById(R.id.endpoint_polarity);
        switch (endpoint.getPolarity()) {
            case SOURCE:
                polarity.setImageResource(R.drawable.ic_endpoint_source_48dp);
                break;
            case SINK:
                polarity.setImageResource(R.drawable.ic_endpoint_sink_48dp);
                break;
        }

        // Set the click handler for the card to signal the activity.
        holder.view.setOnClickListener(view -> {
            if (listener != null) {
                // Notify the active callbacks interface (the activity, if the
                // fragment is attached to one) that an endpoint has been selected.
                listener.onListItemInteraction(endpoint, view);
            }
        });
    }

    @Override
    public int getItemCount() {
        return endpoints.size();
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        View view;

        ViewHolder(View view) {
            super(view);
            this.view = view;
        }
    }
}
