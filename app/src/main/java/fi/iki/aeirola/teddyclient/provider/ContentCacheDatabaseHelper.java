package fi.iki.aeirola.teddyclient.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Helper class that actually creates and manages the provider's underlying data repository.
 */
final class ContentCacheDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = ContentCacheDatabaseHelper.class.getName();

    // Defines the database name
    private static final String DBNAME = "contentCache";

    private static final String[][] SQL_SCHEMA = {
            // Version 0
            {
                    "CREATE TABLE windows (" +
                            TeddyContract.Windows._ID + " INT PRIMARY KEY, " +
                            TeddyContract.Windows.VIEW_ID + " INT, " +
                            TeddyContract.Windows.NAME + " TEXT, " +
                            TeddyContract.Windows.ACTIVITY + " TEXT" +
                            ")",
                    "CREATE TABLE lines(" +
                            TeddyContract.Lines._ID + " INT PRIMARY KEY, " +
                            TeddyContract.Lines.VIEW_ID + " INT, " +
                            TeddyContract.Lines.MESSAGE + " TEXT, " +
                            TeddyContract.Lines.TIMESTAMP + " TEXT" +
                            ")"
            }
    };

    /*
     * Instantiates an open helper for the provider's SQLite data repository
     * Do not do database creation and upgrade here.
     */
    ContentCacheDatabaseHelper(Context context) {
        super(context, DBNAME, null, SQL_SCHEMA.length);
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
