package fi.iki.aeirola.teddyclient.provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Axel on 13.1.2015.
 */
public class TeddyContract {
    public static final String AUTHORITY = "fi.iki.aeirola.teddyclient.provider";
    public static final Uri CONTENT_URI = new Uri.Builder().scheme("content").authority(AUTHORITY).build();

    public static class Windows implements BaseColumns {
        // MIME types
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.fi.iki.aeirola.teddyclient.provider.windows";        // Uris
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.fi.iki.aeirola.teddyclient.provider.windows";
        // Column names
        public static final String VIEW_ID = "view_id";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(TeddyContract.CONTENT_URI, "windows");
        public static final String NAME = "name";
        public static final String ACTIVITY = "activity";

    }

    public static class Lines implements BaseColumns {
        // MIME types
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.fi.iki.aeirola.teddyclient.provider.lines";        // Uris
        // Column names
        public static final String VIEW_ID = "view_id";
        public static final String WINDOW_ID = "window_id";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(TeddyContract.CONTENT_URI, "lines");
        public static final String MESSAGE = "message";
        public static final String TIMESTAMP = "timestamp";
        // Method names
        public static final String UNSYNC = "unsync";

    }
}
