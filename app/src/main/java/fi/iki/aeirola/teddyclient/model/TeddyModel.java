package fi.iki.aeirola.teddyclient.model;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import fi.iki.aeirola.teddyclient.SettingsActivity;
import fi.iki.aeirola.teddyclientlib.TeddyProtocolClient;

/**
 * Helper class for fetching data from the configured teddy instance.
 */
public class TeddyModel {

    private static TeddyModel instance;
    public TeddyProtocolClient teddyProtocolClient;

    private TeddyModel(SharedPreferences sharedPref) {
        String uri = sharedPref.getString(SettingsActivity.KEY_PREF_URI, "");
        String password = sharedPref.getString(SettingsActivity.KEY_PREF_PASSWORD, "");

        teddyProtocolClient = new TeddyProtocolClient(uri, password);

        if (!uri.isEmpty()) {
            Log.v("TeddyModel", "Connecting to " + uri);
            teddyProtocolClient.connect();
        }
    }

    public static TeddyModel getInstance(Fragment fragment) {
        return TeddyModel.getInstance(fragment.getActivity());
    }

    public static TeddyModel getInstance(Context context) {
        if (instance == null) {
            Log.v("TeddyModel", "Creating new instance of model");
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            instance = new TeddyModel(pref);
        }

        return instance;
    }
}
