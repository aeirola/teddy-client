package fi.iki.aeirola.teddyclient.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;

import fi.iki.aeirola.teddyclientlib.TeddyCallbackHandler;
import fi.iki.aeirola.teddyclientlib.TeddyClient;
import fi.iki.aeirola.teddyclientlib.models.Line;
import fi.iki.aeirola.teddyclientlib.models.Window;

/**
 * Created by Axel on 10.1.2015.
 */
public class TeddyContentProvider extends ContentProvider {
    private static final String TAG = TeddyContentProvider.class.getName();

    private static final int WINDOWS_URI_ID = 1;
    private static final int WINDOW_URI_ID = 2;
    private static final int LINE_URI_ID = 3;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(TeddyContract.AUTHORITY, "windows", WINDOWS_URI_ID);
        sUriMatcher.addURI(TeddyContract.AUTHORITY, "windows/#", WINDOW_URI_ID);
        sUriMatcher.addURI(TeddyContract.AUTHORITY, "lines", LINE_URI_ID);
    }

    private static final DateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    // For throttling window sync interval
    private static final long WINDOW_UPDATE_INTERVAL = 15000L;
    private final HashSet<Long> syncs = new HashSet<>();
    private long previousWindowSyncMs = 0;
    /*
     * Defines a handle to the database helper object. The MainDatabaseHelper class is defined
     * in a following snippet.
     */
    private ContentCacheDatabaseHelper mOpenHelper;
    private TeddyClient mTeddyClient;

    @Override
    public boolean onCreate() {
        /*
         * Creates a new helper object. This method always returns quickly.
         * Notice that the database itself isn't created or opened
         * until SQLiteOpenHelper.getWritableDatabase is called
         */
        mOpenHelper = new ContentCacheDatabaseHelper(getContext());
        mTeddyClient = TeddyClient.getInstance(getContext());
        mTeddyClient.registerCallbackHandler(new TeddyCallbackHelper(), "TeddyContentProvider");

        try {
            // XXX: Should retain database
            getContext().deleteDatabase("contentCache");
        } catch (Exception e) {
        }

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selections, String[] selectionArgs, String sortOrder) {
        switch (sUriMatcher.match(uri)) {
            case WINDOWS_URI_ID:
                return queryWindows(uri, projection, selections, selectionArgs, sortOrder);
            case WINDOW_URI_ID:
                return queryWindow(uri, projection, selections, selectionArgs, sortOrder);
            case LINE_URI_ID:
                return queryLines(uri, projection, selections, selectionArgs, sortOrder);
            default:
                throw new UnsupportedOperationException("Query " + uri);
        }
    }

    private Cursor queryWindows(Uri uri, String[] projection, String selections, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        if (System.currentTimeMillis() > (previousWindowSyncMs + WINDOW_UPDATE_INTERVAL)) {
            mTeddyClient.requestWindowList();
        }

        Cursor cursor = db.query("windows", projection, selections, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), TeddyContract.Windows.CONTENT_URI);
        return cursor;
    }

    private Cursor queryWindow(Uri uri, String[] projection, String selections, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        long windowId = ContentUris.parseId(uri);
        selections = TeddyContract.Windows._ID + " = ?";
        selectionArgs = new String[]{String.valueOf(windowId)};
        return db.query("windows", projection, selections, selectionArgs, null, null, null, "1");
    }

    private Cursor queryLines(Uri uri, String[] projection, String selections, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        long viewId;
        if (selections.equals(TeddyContract.Lines.VIEW_ID + " = ?")) {
            viewId = Long.valueOf(selectionArgs[0]);
        } else {
            throw new UnsupportedOperationException("Cannot parse view id from selection");
        }

        if (sortOrder == null) {
            sortOrder = "timestamp ASC";
        }

        String limit = "100";

        if (!syncs.contains(viewId)) {
            mTeddyClient.subscribeLines(viewId);
            mTeddyClient.requestLineList(viewId, Integer.valueOf(limit));
            syncs.add(viewId);
        }

        Cursor cursor = db.query("lines", projection, selections, selectionArgs, null, null, sortOrder, limit);
        cursor.setNotificationUri(getContext().getContentResolver(), TeddyContract.Lines.CONTENT_URI);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case WINDOWS_URI_ID:
                return TeddyContract.Windows.CONTENT_TYPE;
            case WINDOW_URI_ID:
                return TeddyContract.Windows.CONTENT_ITEM_TYPE;
            case LINE_URI_ID:
                return TeddyContract.Lines.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Get type " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        switch (sUriMatcher.match(uri)) {
            case LINE_URI_ID:
                long windowId = contentValues.getAsLong(TeddyContract.Lines.WINDOW_ID);
                String message = contentValues.getAsString(TeddyContract.Lines.MESSAGE);
                mTeddyClient.sendInput(windowId, message);
                return ContentUris.withAppendedId(uri, 0);
            default:
                throw new UnsupportedOperationException("Insert " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        throw new UnsupportedOperationException("Delete " + uri);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selections, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (sUriMatcher.match(uri)) {
            case WINDOW_URI_ID:
                long windowId = ContentUris.parseId(uri);
                selections = TeddyContract.Windows._ID + " = ?";
                selectionArgs = new String[]{String.valueOf(windowId)};
                int returnValue = db.update("windows", contentValues, selections, selectionArgs);

                if (Window.Activity.INACTIVE.name().equals(contentValues.getAsString(TeddyContract.Windows.ACTIVITY))) {
                    mTeddyClient.resetWindowActivity(windowId);
                }
                return returnValue;
            default:
                throw new UnsupportedOperationException("Update " + uri);
        }
    }

    private void updateWindows(List<Window> windowList) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // TODO: Forget old windows
            for (Window window : windowList) {
                ContentValues values = new ContentValues();
                values.put(TeddyContract.Windows._ID, window.id);
                values.put(TeddyContract.Windows.VIEW_ID, window.viewId);
                values.put(TeddyContract.Windows.NAME, window.name);
                values.put(TeddyContract.Windows.ACTIVITY, window.activity.toString());
                db.insertWithOnConflict("windows", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        previousWindowSyncMs = System.currentTimeMillis();
        getContext().getContentResolver().notifyChange(TeddyContract.Windows.CONTENT_URI, null, false);
    }

    private void updateLines(List<Line> lineList) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // TODO: Remove old lines and long scrollbacks
            // TODO: Add more robust subsecond ordering
            for (Line line : lineList) {
                ContentValues values = new ContentValues();
                values.put(TeddyContract.Lines._ID, line.id);
                values.put(TeddyContract.Lines.VIEW_ID, line.viewId);
                values.put(TeddyContract.Lines.MESSAGE, line.message);
                values.put(TeddyContract.Lines.TIMESTAMP, SQL_DATE_FORMAT.format(line.timestamp));
                db.insertWithOnConflict("lines", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        getContext().getContentResolver().notifyChange(TeddyContract.Lines.CONTENT_URI, null, false);
    }

    protected final class TeddyCallbackHelper extends TeddyCallbackHandler {
        @Override
        public void onWindowList(List<Window> windowList) {
            updateWindows(windowList);
        }

        @Override
        public void onLineList(final List<Line> lineList) {
            updateLines(lineList);
        }

        @Override
        public void onNewLines(final List<Line> lineList) {
            updateLines(lineList);
        }

        @Override
        public void onReconnect() {
            // TODO: Re-sync on reconnects
        }
    }
}
