package fi.iki.aeirola.teddyclient.utils;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import fi.iki.aeirola.teddyclientlib.TeddyCallbackHandler;
import fi.iki.aeirola.teddyclientlib.TeddyClient;

/**
 * Created by Axel on 21.2.2015.
 */
public class TeddyToaster extends TeddyCallbackHandler {
    private static final String TAG = TeddyToaster.class.getName();
    private static TeddyToaster toaster;

    private TeddyClient teddyClient;
    private Context context;
    private Handler mainHandler;

    public TeddyToaster(Context context) {
        this.context = context;
        this.teddyClient = TeddyClient.getInstance(context);
        this.mainHandler = new Handler(context.getMainLooper());

        teddyClient.registerCallbackHandler(this, TAG);
    }

    public static void init(Context context) {
        if (toaster == null) {
            toaster = new TeddyToaster(context);
        }
    }

    private void toast(final String text) {
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(TeddyToaster.this.context, text, Toast.LENGTH_SHORT).show();
            }
        };
        mainHandler.post(myRunnable);
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
}
