package fi.iki.aeirola.teddyclient;

import android.app.Application;

import fi.iki.aeirola.teddyclient.utils.TeddyToaster;

/**
 * Created by Axel on 21.2.2015.
 */
public class TeddyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        TeddyToaster.init(getApplicationContext());
    }
}
