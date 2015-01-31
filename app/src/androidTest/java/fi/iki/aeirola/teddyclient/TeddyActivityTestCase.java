package fi.iki.aeirola.teddyclient;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

import java.net.InetSocketAddress;

import fi.iki.aeirola.teddyclientlib.TestServer;

/**
 * Created by Axel on 31.1.2015.
 */
public abstract class TeddyActivityTestCase<T extends Activity> extends ActivityInstrumentationTestCase2<T> {

    protected Solo solo;
    private TestServer server;

    public TeddyActivityTestCase(Class<T> activityClass) {
        super(activityClass);
    }

    @Override
    public void setUp() throws Exception {
        this.server = new TestServer(new InetSocketAddress("localhost", 8080));
        this.server.start();
        Thread.sleep(100);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getInstrumentation().getTargetContext());
        preferences.edit().putString(SettingsActivity.KEY_PREF_URI, "ws://localhost:8080/teddy").apply();

        Solo.Config config = new Solo.Config();
        config.screenshotFileType = Solo.Config.ScreenshotFileType.PNG;
        config.screenshotSavePath = Environment.getExternalStorageDirectory() + "/test_screenshots/";
        solo = new Solo(getInstrumentation(), config, getActivity());
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
        this.server.stop();
        this.server = null;
    }
}
