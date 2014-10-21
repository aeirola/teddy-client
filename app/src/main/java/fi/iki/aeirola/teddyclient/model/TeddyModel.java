package fi.iki.aeirola.teddyclient.model;

import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import java.util.List;

import fi.iki.aeirola.teddyclient.SettingsActivity;
import fi.iki.aeirola.teddyclientlib.TeddyProtocolCallbackHandler;
import fi.iki.aeirola.teddyclientlib.TeddyProtocolClient;
import fi.iki.aeirola.teddyclientlib.models.Window;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class TeddyModel {

    private static TeddyModel instance;
    private final TeddyProtocolCallbackHandler callbackHandler;
    private final Handler mHandler;
    public TeddyProtocolClient teddyProtocolClient;

    private TeddyModel(Handler mHandler, SharedPreferences sharedPref) {
        this.mHandler = mHandler;

        String uri = sharedPref.getString(SettingsActivity.KEY_PREF_URI, "");
        String password = sharedPref.getString(SettingsActivity.KEY_PREF_PASSWORD, "");

        this.callbackHandler = new TeddyProtocolCallbackHandler() {
            @Override
            public void onWindowList(List<Window> windowList) {
                Log.v("TeddyModel", "windows received!");
                TeddyModel.this.mHandler.obtainMessage(1, windowList).sendToTarget();
            }
        };

        teddyProtocolClient = new TeddyProtocolClient(uri, password);
        teddyProtocolClient.registerCallbackHandler(this.callbackHandler, "TeddyModel");

        if (!uri.isEmpty()) {
            Log.v("TeddyModel", "Connecting to " + uri);
            teddyProtocolClient.connect();
        }
    }

    public static TeddyModel getInstance(Handler mHandler, SharedPreferences sharedPref) {
        if (instance == null) {
            Log.v("TeddyModel", "Creating new instance of model");
            instance = new TeddyModel(mHandler, sharedPref);
        }

        return instance;
    }
}
