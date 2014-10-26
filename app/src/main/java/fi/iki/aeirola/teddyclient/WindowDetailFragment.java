package fi.iki.aeirola.teddyclient;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import fi.iki.aeirola.teddyclientlib.TeddyCallbackHandler;
import fi.iki.aeirola.teddyclientlib.TeddyClient;
import fi.iki.aeirola.teddyclientlib.models.Line;
import fi.iki.aeirola.teddyclientlib.models.Window;

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
    public static final String ARG_WINDOW = "item_id";
    private static final String TAG = WindowDetailFragment.class.getName();

    /**
     * The dummy content this fragment is presenting.
     */
    private TeddyClient mTeddyClient;
    private Window window;
    private EditText mEditText;
    private ArrayAdapter mListAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public WindowDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.mTeddyClient = TeddyClient.getInstance(getActivity());
        this.mTeddyClient.connect();
        this.mListAdapter = new ArrayAdapter<Line>(
                getActivity(),
                android.R.layout.simple_list_item_activated_1,
                android.R.id.text1,
                new ArrayList<Line>());
        setListAdapter(this.mListAdapter);

        mTeddyClient.registerCallbackHandler(new TeddyCallbackHandler() {
            @Override
            public void onLineList(List<Line> lineList) {
                Iterator<Line> lineIterator = lineList.iterator();
                while (lineIterator.hasNext()) {
                    if (lineIterator.next().windowId != WindowDetailFragment.this.window.id) {
                        lineIterator.remove();
                    }
                }
                Collections.reverse(lineList);
                mListAdapter.addAll(lineList);
                WindowDetailFragment.this.scrollToBottom();
            }
        }, TAG);
    }

    private void scrollToBottom() {
        setSelection(mListAdapter.getCount() - 1);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_window_detail, null);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEditText = (EditText) getActivity().findViewById(R.id.window_detail_input);
        if (mEditText == null) {
            return;
        }
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage();
                    handled = true;
                }
                return handled;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        mTeddyClient.enableSync();

        if (getArguments().containsKey(ARG_WINDOW)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            this.window = (Window) getArguments().getSerializable(ARG_WINDOW);
            mTeddyClient.requestLineList(this.window.id, 20);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        mTeddyClient.disableSync();
    }


    private void sendMessage() {
        if (this.window == null) {
            return;
        }
        String message = mEditText.getText().toString();
        this.mTeddyClient.sendInput(window.fullName, message);
        mEditText.setText("");
    }
}
