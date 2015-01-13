package fi.iki.aeirola.teddyclient.fragments;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
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

import fi.iki.aeirola.teddyclient.R;
import fi.iki.aeirola.teddyclient.provider.TeddyContract;
import fi.iki.aeirola.teddyclient.views.adapters.IrssiLineAdapter;
import fi.iki.aeirola.teddyclientlib.TeddyCallbackHandler;
import fi.iki.aeirola.teddyclientlib.TeddyClient;
import fi.iki.aeirola.teddyclientlib.models.Line;
import fi.iki.aeirola.teddyclientlib.models.request.LineRequest;

/**
 * A fragment representing a single Window detail screen.
 * This fragment is either contained in a {@link fi.iki.aeirola.teddyclient.WindowListActivity}
 * in two-pane mode (on tablets) or a {@link fi.iki.aeirola.teddyclient.WindowDetailActivity}
 * on handsets.
 */
public class WindowDetailFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_WINDOW = "window_id";
    private static final String TAG = WindowDetailFragment.class.getName();
    private TeddyClient mTeddyClient;
    private EditText mEditText;
    private ArrayAdapter<Line> mListAdapter;
    private boolean fetchingMoreLines = false;
    private long windowId = 0;
    private long viewId = 0;

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

                filterLines(lineList);

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

                mTeddyClient.resetWindowActivity(windowId);
            }

            @Override
            public void onNewLines(final List<Line> lineList) {
                if (!isVisible() || lineList == null || lineList.isEmpty()) {
                    return;
                }

                filterLines(lineList);

                // New lines, added at bottom of list
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mListAdapter.addAll(lineList);

                        // Only scroll if we are near the bottom
                        ListView listView = getListView();
                        int last = listView.getLastVisiblePosition();
                        int size = listView.getCount();
                    }
                });

                // Reset activity for window
                mTeddyClient.resetWindowActivity(windowId);
            }

            private void filterLines(List<Line> lineList) {
                Iterator<Line> lineIterator = lineList.iterator();
                while (lineIterator.hasNext()) {
                    if (lineIterator.next().viewId != WindowDetailFragment.this.viewId) {
                        lineIterator.remove();
                    }
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
                mTeddyClient.requestLineList(viewId, lineRequest);
            }
        }, TAG);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_window_detail, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getArguments().containsKey(ARG_WINDOW)) {
            windowId = getArguments().getLong(ARG_WINDOW);
            Log.i(TAG, "Opening window " + windowId);
            getLoaderManager().initLoader(0, null, this);
        } else {
            Log.w(TAG, "Window argument not found!");
        }
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
                    mTeddyClient.requestLineList(viewId, lineRequest);
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
    }

    @Override
    public void onStop() {
        super.onStop();

        mTeddyClient.unsubscribeLines(viewId);
    }

    private void sendMessage() {
        if (windowId == 0) {
            return;
        }
        String message = mEditText.getText().toString();
        this.mTeddyClient.sendInput(windowId, message);
        mEditText.setText("");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // This is called when a new Loader needs to be created.
        String[] mProjection = {
                TeddyContract.Windows._ID,
                TeddyContract.Windows.VIEW_ID,
                TeddyContract.Windows.NAME,
        };
        Uri windowUri = ContentUris.withAppendedId(TeddyContract.Windows.CONTENT_URI, windowId);
        return new CursorLoader(getActivity(), windowUri, mProjection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> objectLoader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        if (data == null || !data.moveToFirst()) {
            return;
        }

        viewId = data.getLong(data.getColumnIndex(TeddyContract.Windows.VIEW_ID));

        getActivity().setTitle(data.getString(data.getColumnIndex(TeddyContract.Windows.NAME)));
        mTeddyClient.subscribeLines(viewId);
        mTeddyClient.requestLineList(viewId, 50);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> objectLoader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
    }
}
