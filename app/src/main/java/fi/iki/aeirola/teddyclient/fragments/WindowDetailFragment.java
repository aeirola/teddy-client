package fi.iki.aeirola.teddyclient.fragments;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import fi.iki.aeirola.teddyclient.R;
import fi.iki.aeirola.teddyclient.provider.TeddyContract;
import fi.iki.aeirola.teddyclient.views.adapters.IrssiLineAdapter;
import fi.iki.aeirola.teddyclientlib.models.Window;

/**
 * A fragment representing a single Window detail screen.
 * This fragment is either contained in a {@link fi.iki.aeirola.teddyclient.WindowListActivity}
 * in two-pane mode (on tablets) or a {@link fi.iki.aeirola.teddyclient.WindowDetailActivity}
 * on handsets.
 */
public class WindowDetailFragment extends ListFragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_WINDOW = "window_id";
    private static final String TAG = WindowDetailFragment.class.getName();
    private EditText mEditText;
    private IrssiLineAdapter dataAdapter;
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

        this.dataAdapter = new IrssiLineAdapter(getActivity(), null);
        setListAdapter(this.dataAdapter);
    }

    private void resetWindowActivity(long windowId) {
        ContentValues newValues = new ContentValues();
        newValues.put(TeddyContract.Windows.ACTIVITY, Window.Activity.INACTIVE.toString());
        Uri uri = ContentUris.withAppendedId(TeddyContract.Windows.CONTENT_URI, windowId);
        getActivity().getContentResolver().update(uri, newValues, null, null);
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
            getLoaderManager().initLoader(WindowLoaderCallback.LOADER_ID, null, new WindowLoaderCallback());
        } else {
            Log.w(TAG, "Window argument not found!");
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // TODO: Fetch more line when reaching end of list

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

        if (viewId != 0) {
            getLoaderManager().restartLoader(LinesLoaderCallback.LOADER_ID, null, new LinesLoaderCallback());
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        getActivity().getContentResolver().call(TeddyContract.Lines.CONTENT_URI, TeddyContract.Lines.UNSYNC, String.valueOf(viewId), null);
    }

    private void sendMessage() {
        if (windowId == 0) {
            return;
        }
        Editable text = mEditText.getText();
        if (text.length() == 0) {
            return;
        }

        ContentValues newValues = new ContentValues();
        newValues.put(TeddyContract.Lines.MESSAGE, text.toString());
        newValues.put(TeddyContract.Lines.WINDOW_ID, windowId);
        getActivity().getContentResolver().insert(TeddyContract.Lines.CONTENT_URI, newValues);

        text.clear();
    }

    private class WindowLoaderCallback implements LoaderManager.LoaderCallbacks<Cursor> {
        private static final int LOADER_ID = 1;

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
            getLoaderManager().initLoader(LinesLoaderCallback.LOADER_ID, null, new LinesLoaderCallback());
        }

        @Override
        public void onLoaderReset(Loader<Cursor> objectLoader) {
            // This is called when the last Cursor provided to onLoadFinished()
            // above is about to be closed.  We need to make sure we are no
            // longer using it.
        }
    }

    private class LinesLoaderCallback implements LoaderManager.LoaderCallbacks<Cursor> {
        private static final int LOADER_ID = 2;

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            // This is called when a new Loader needs to be created.
            String[] mProjection = {
                    TeddyContract.Lines._ID,
                    TeddyContract.Lines.MESSAGE,
            };
            String mSelection = TeddyContract.Lines.VIEW_ID + " = ?";
            String[] mSelectionArgs = {
                    String.valueOf(viewId)
            };

            return new CursorLoader(getActivity(), TeddyContract.Lines.CONTENT_URI, mProjection, mSelection, mSelectionArgs, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> objectLoader, Cursor data) {
            // Swap the new cursor in.  (The framework will take care of closing the
            // old cursor once we return.)
            dataAdapter.swapCursor(data);

            // Mark that this windows has been seen
            resetWindowActivity(windowId);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> objectLoader) {
            // This is called when the last Cursor provided to onLoadFinished()
            // above is about to be closed.  We need to make sure we are no
            // longer using it.
            dataAdapter.swapCursor(null);
        }
    }
}
