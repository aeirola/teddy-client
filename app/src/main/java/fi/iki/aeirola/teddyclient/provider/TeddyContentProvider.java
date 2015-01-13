package fi.iki.aeirola.teddyclient.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import java.util.List;

import fi.iki.aeirola.teddyclientlib.TeddyCallbackHandler;
import fi.iki.aeirola.teddyclientlib.TeddyClient;
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

    // For throttling window sync interval
    private static final long WINDOW_UPDATE_INTERVAL = 15000L;
    // Defines the database name
    private static final String DBNAME = "contentCache";
    private long previousWindowSyncMs = 0;
    /*
     * Defines a handle to the database helper object. The MainDatabaseHelper class is defined
     * in a following snippet.
     */
    private MainDatabaseHelper mOpenHelper;
    private TeddyClient mTeddyClient;

    @Override
    public boolean onCreate() {
        /*
         * Creates a new helper object. This method always returns quickly.
         * Notice that the database itself isn't created or opened
         * until SQLiteOpenHelper.getWritableDatabase is called
         */
        mOpenHelper = new MainDatabaseHelper(getContext());
        mTeddyClient = TeddyClient.getInstance(getContext());
        mTeddyClient.registerCallbackHandler(new TeddyCallbackHelper(), "TeddyContentProvider");

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selections, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor cursor = null;
        switch (sUriMatcher.match(uri)) {
            case WINDOWS_URI_ID:
                if (System.currentTimeMillis() > (previousWindowSyncMs + WINDOW_UPDATE_INTERVAL)) {
                    mTeddyClient.requestWindowList();
                }
                cursor = db.query("windows", projection, selections, selectionArgs, null, null, sortOrder);
                cursor.setNotificationUri(getContext().getContentResolver(), TeddyContract.Windows.CONTENT_URI);
                break;
            case WINDOW_URI_ID:
                long windowId = ContentUris.parseId(uri);
                selections = TeddyContract.Windows._ID + " = ?";
                selectionArgs = new String[]{String.valueOf(windowId)};
                return db.query("windows", projection, selections, selectionArgs, null, null, null, "1");
            case LINE_URI_ID:
                return db.query("lines", projection, selections, selectionArgs, null, null, sortOrder);
            default:
                throw new UnsupportedOperationException("Query " + uri);
        }
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
        throw new UnsupportedOperationException("Insert " + uri);
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
                int retval = db.update("windows", contentValues, selections, selectionArgs);

                if (Window.Activity.INACTIVE.name().equals(contentValues.getAsString(TeddyContract.Windows.ACTIVITY))) {
                    mTeddyClient.resetWindowActivity(windowId);
                }
                return retval;
            default:
                throw new UnsupportedOperationException("Update " + uri);
        }
    }

    /**
     * Helper class that actually creates and manages the provider's underlying data repository.
     */
    protected static final class MainDatabaseHelper extends SQLiteOpenHelper {
        private static final String[][] SQL_SCHEMA = {
                // Version 0
                {
                        "CREATE TABLE windows (" +
                                TeddyContract.Windows._ID + " INT PRIMARY KEY, " +
                                TeddyContract.Windows.VIEW_ID + " INT, " +
                                TeddyContract.Windows.NAME + " TEXT, " +
                                TeddyContract.Windows.ACTIVITY + " TEXT" +
                                ")"
                }
        };

        /*
         * Instantiates an open helper for the provider's SQLite data repository
         * Do not do database creation and upgrade here.
         */
        MainDatabaseHelper(Context context) {
            super(context, DBNAME, null, 1);
        }

        /*
         * Creates the data repository. This is called when the provider attempts to open the
         * repository and SQLite reports that it doesn't exist.
         */
        public void onCreate(SQLiteDatabase db) {
            Log.i(TAG, "Creating database");
            migrateToVersion(db, 0);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.i(TAG, "Upgrading database from " + oldVersion + " to " + newVersion);
            for (int i = oldVersion; i <= newVersion; i++) {
                migrateToVersion(db, i);
            }
        }

        private void migrateToVersion(SQLiteDatabase db, int schemaVersion) {
            Log.v(TAG, "Migrating to version " + schemaVersion);
            db.beginTransaction();
            try {
                // Run statements in first schema version
                for (String command : SQL_SCHEMA[schemaVersion]) {
                    db.execSQL(command);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    protected final class TeddyCallbackHelper extends TeddyCallbackHandler {
        @Override
        public void onWindowList(List<Window> windowList) {
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            db.beginTransaction();
            try {
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
    }
}
