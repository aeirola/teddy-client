package fi.iki.aeirola.teddyclient;

import android.app.ListFragment;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import java.util.List;

import fi.iki.aeirola.teddyclient.model.TeddyModel;
import fi.iki.aeirola.teddyclientlib.TeddyProtocolCallbackHandler;
import fi.iki.aeirola.teddyclientlib.models.Line;

/**
 * A fragment representing a single Window detail screen.
 * This fragment is either contained in a {@link WindowListActivity}
 * in two-pane mode (on tablets) or a {@link WindowDetailActivity}
 * on handsets.
 */
public class WindowDetailFragment extends ListFragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_WINDOW_ID = "item_id";

    /**
     * The dummy content this fragment is presenting.
     */
    private TeddyModel teddyModel;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public WindowDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.teddyModel = TeddyModel.getInstance(this);
        teddyModel.teddyProtocolClient.registerCallbackHandler(new TeddyProtocolCallbackHandler() {
            @Override
            public void onLineList(List<Line> lineList) {
                setListAdapter(new ArrayAdapter<Line>(
                        getActivity(),
                        android.R.layout.simple_list_item_activated_1,
                        android.R.id.text1,
                        lineList));
            }
        }, "WindowListFragment");

        if (getArguments().containsKey(ARG_WINDOW_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            teddyModel.teddyProtocolClient.requestLineList(getArguments().getLong(ARG_WINDOW_ID));
        }
    }
}
