package fi.iki.aeirola.teddyclient.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import fi.iki.aeirola.teddyclientlib.TeddyCallbackHandler;
import fi.iki.aeirola.teddyclientlib.TeddyClient;

/**
 * Created by Axel on 21.2.2015.
 */
public class TeddyToaster extends TeddyCallbackHandler implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = TeddyToaster.class.getName();
    private static TeddyToaster toaster;

    private TeddyClient teddyClient;
    private Application application;
    private Handler mainHandler;
    private Toast previousToast;

    private int started = 0;
    private int resumed = 0;

    public TeddyToaster(Application application) {
        this.application = application;
        this.teddyClient = TeddyClient.getInstance(application);
        this.mainHandler = new Handler(application.getMainLooper());

        teddyClient.registerCallbackHandler(this, TAG);
        application.registerActivityLifecycleCallbacks(this);
    }

    public static void init(Application context) {
        if (toaster == null) {
            toaster = new TeddyToaster(context);
        }
    }

    private void toast(final String text) {
        if (!this.isActive()) {
            return;
        }

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                if (!TeddyToaster.this.isActive()) {
                    return;
                }

                if (TeddyToaster.this.previousToast != null) {
                    TeddyToaster.this.previousToast.cancel();
                }

                TeddyToaster.this.previousToast = Toast.makeText(TeddyToaster.this.application, text, Toast.LENGTH_SHORT);
                TeddyToaster.this.previousToast.show();
            }
        };
        mainHandler.post(myRunnable);
    }

    private boolean isActive() {
        return TeddyToaster.this.started > 0 && TeddyToaster.this.resumed > 0;
    }

    @Override
    public void onConnect() {
        toast("Connected");
    }

    @Override
    public void onDisconnect() {
        toast("Disconnected");
    }

    @Override
    public void onReconnect() {
        toast("Reconnected");
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        started++;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        resumed++;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        resumed--;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        started--;
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}
