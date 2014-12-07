package fi.iki.aeirola.teddyclient;

import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Iterator;
import java.util.List;

import fi.iki.aeirola.teddyclient.utils.IrssiLineAdapter;
import fi.iki.aeirola.teddyclientlib.TeddyCallbackHandler;
import fi.iki.aeirola.teddyclientlib.TeddyClient;
import fi.iki.aeirola.teddyclientlib.models.Line;
import fi.iki.aeirola.teddyclientlib.models.Window;
import fi.iki.aeirola.teddyclientlib.models.request.LineRequest;

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
    private ArrayAdapter<Line> mListAdapter;
    private boolean fetchingMoreLines = false;

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
        this.mListAdapter = new IrssiLineAdapter(getActivity());
        setListAdapter(this.mListAdapter);

        mTeddyClient.registerCallbackHandler(new TeddyCallbackHandler() {
            @Override
            public void onLineList(final List<Line> lineList) {
                if (!isVisible() || lineList == null || lineList.isEmpty()) {
                    return;
                }
                Iterator<Line> lineIterator = lineList.iterator();
                while (lineIterator.hasNext()) {
                    if (lineIterator.next().viewId != WindowDetailFragment.this.window.viewId) {
                        lineIterator.remove();
                    }
                }

                if (mListAdapter.isEmpty() || lineList.get(0).date.after(mListAdapter.getItem(0).date)) {
                    // New lines, added at bottom of list
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mListAdapter.addAll(lineList);

                            // Only scroll if we are near the bottom
                            ListView listView = getListView();
                            int last = listView.getLastVisiblePosition();
                            int size = listView.getCount();
                            if (last < 0 || size - last <= 3) {
                                WindowDetailFragment.this.scrollToBottom();
                            }
                        }
                    });

                    // Reset activity for window
                    mTeddyClient.resetWindowActivity(window.id);
                } else {
                    // Old lines, added at top of list
                    fetchingMoreLines = false;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Add in reverse
                            for (int i = lineList.size() - 1; i >= 0; i--) {
                                mListAdapter.insert(lineList.get(i), 0);
                            }
                            setSelection(lineList.size());
                        }
                    });
                }
            }

            @Override
            public void onReconnect() {
                if (!isVisible()) {
                    return;
                }

                LineRequest.Get lineRequest = new LineRequest.Get();
                if (!mListAdapter.isEmpty()) {
                    lineRequest.afterLine = mListAdapter.getItem(mListAdapter.getCount() - 1).id;
                } else {
                    lineRequest.count = 50;
                }
                mTeddyClient.requestLineList(window.viewId, lineRequest);
            }
        }, TAG);
    }

    private void scrollToBottom() {
        setSelection(mListAdapter.getCount() - 1);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_window_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (!fetchingMoreLines && visibleItemCount != 0 && firstVisibleItem == 0) {
                    LineRequest.Get lineRequest = new LineRequest.Get();
                    lineRequest.beforeLine = mListAdapter.getItem(0).id;
                    lineRequest.count = 50;
                    mTeddyClient.requestLineList(window.viewId, lineRequest);
                    fetchingMoreLines = true;
                }
            }
        });

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

        mListAdapter.clear();

        if (getArguments().containsKey(ARG_WINDOW)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            this.window = (Window) getArguments().getSerializable(ARG_WINDOW);
            mTeddyClient.subscribeLines(window.viewId);
            mTeddyClient.requestLineList(window.viewId, 50);
        } else {
            Log.w(TAG, "Window argument not found!");
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        mTeddyClient.unsubscribeLines(window.viewId);
    }


    private void sendMessage() {
        if (this.window == null) {
            return;
        }
        String message = mEditText.getText().toString();
        this.mTeddyClient.sendInput(window.id, message);
        mEditText.setText("");
    }
}
