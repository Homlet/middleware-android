package uk.ac.cam.seh208.middleware.demo;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import uk.ac.cam.seh208.middleware.common.EndpointDetails;


public class EndpointListAdapter extends RecyclerView.Adapter<EndpointListAdapter.ViewHolder> {

    class ViewHolder extends RecyclerView.ViewHolder {
        View view;

        @BindView(R.id.endpoint_name)
        TextView textName;

        @BindView(R.id.endpoint_desc)
        TextView textDesc;

        @BindView(R.id.endpoint_polarity)
        ImageView polarity;


        ViewHolder(View view) {
            super(view);
            this.view = view;
            ButterKnife.bind(this, view);
        }
    }


    private final List<EndpointDetails> detailsList;

    private final EndpointListFragment.OnListItemInteractionListener listener;


    public EndpointListAdapter(List<EndpointDetails> detailsList,
                               EndpointListFragment.OnListItemInteractionListener listener) {
        this.detailsList = detailsList;
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
        // Get the endpoint details.
        synchronized (detailsList) {
            EndpointDetails details = detailsList.get(position);

            // Fill in the details in the card.
            holder.textName.setText(details.getName());
            holder.textDesc.setText(details.getDesc());
            holder.polarity.setImageResource(
                    ResourceUtils.getPolarityImageResource(details.getPolarity()));

            // Set the click handler for the card to signal the activity.
            holder.view.setOnClickListener(v -> {
                if (listener != null) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an endpoint has been selected.
                    listener.onListItemInteraction(details, v);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return detailsList.size();
    }
}
