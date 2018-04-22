package uk.ac.cam.seh208.middleware.demo;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;


public class MetricsFragment extends Fragment {

    /**
     * Back-reference to the owning activity.
     */
    private MainActivity mainActivity;

    @BindView(R.id.switch_middleware_server)
    Switch switchMiddlewareServer;

    @BindView(R.id.switch_zeromq_server)
    Switch switchZMQServer;

    @BindView(R.id.switch_tcp_ip_server)
    Switch switchTCPServer;


    /**
     * Create a view and inflate it with an options list.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_metrics, container, false);
        ButterKnife.bind(this, view);

        if (mainActivity != null) {
            switchMiddlewareServer.setChecked(mainActivity.getMiddlewareServer().isStarted());
            switchZMQServer.setChecked(mainActivity.getZMQServer().isStarted());
            switchTCPServer.setChecked(mainActivity.getTCPServer().isStarted());
        }

        return view;
    }

    /**
     * Store a reference to the parent activity, in order to signal events to it.
     *
     * Called when the fragment object is attached to a context, such as the main activity.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Store a reference to the parent activity.
        mainActivity = (MainActivity) context;
    }

    /**
     * Forget the previously referenced parent activity.
     *
     * Called when the fragment object is detached from a context, such as the main activity.
     */
    @Override
    public void onDetach() {
        super.onDetach();

        // Drop the reference to the main activity.
        mainActivity = null;
    }

    @OnCheckedChanged(R.id.switch_middleware_server)
    void onMiddlewareSwitchChanged(CompoundButton button, boolean checked) {
        if (checked) {
            mainActivity.getMiddlewareServer().start();
        } else {
            mainActivity.getMiddlewareServer().stop();
        }
    }

    @OnCheckedChanged(R.id.switch_zeromq_server)
    void onZMQSwitchChanged(CompoundButton button, boolean checked) {
        if (checked) {
            mainActivity.getZMQServer().start();
        } else {
            mainActivity.getZMQServer().stop();
        }
    }

    @OnCheckedChanged(R.id.switch_tcp_ip_server)
    void onTCPSwitchChanged(CompoundButton button, boolean checked) {
        if (checked) {
            mainActivity.getTCPServer().start();
        } else {
            mainActivity.getTCPServer().stop();
        }
    }

    @OnClick(R.id.button_middleware_metrics)
    void onMiddlewareButtonClicked() {
        mainActivity.runMiddlewareMetrics(100, 50);
    }
}
