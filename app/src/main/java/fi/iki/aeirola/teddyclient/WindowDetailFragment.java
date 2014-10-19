package fi.iki.aeirola.teddyclient;

import android.app.ListFragment;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


import fi.iki.aeirola.teddyclient.model.TeddyModel;
import fi.iki.aeirola.teddyclient.model.WindowModel;

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
    private WindowModel mItem;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public WindowDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_WINDOW_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.

        }
    }
}
