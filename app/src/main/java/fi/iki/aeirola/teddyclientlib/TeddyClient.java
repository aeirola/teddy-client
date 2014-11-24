package fi.iki.aeirola.teddyclientlib;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import fi.iki.aeirola.teddyclient.SettingsActivity;
import fi.iki.aeirola.teddyclientlib.models.Line;
import fi.iki.aeirola.teddyclientlib.models.Window;
import fi.iki.aeirola.teddyclientlib.models.request.InfoRequest;
import fi.iki.aeirola.teddyclientlib.models.request.InputRequest;
import fi.iki.aeirola.teddyclientlib.models.request.ItemRequest;
import fi.iki.aeirola.teddyclientlib.models.request.LineRequest;
import fi.iki.aeirola.teddyclientlib.models.request.Request;
import fi.iki.aeirola.teddyclientlib.models.request.WindowRequest;

/**
 * Created by aeirola on 14.10.2014.
 */
public class TeddyClient {
    private static final String TAG = TeddyClient.class.getName();
    private static TeddyClient instance;
    private final Queue<Object> messageQueue = new ArrayDeque<>();
    private final Map<String, TeddyCallbackHandler> callbackHandlers = new HashMap<>();
    private TeddyConnectionHandler mConnectionHandler;
    private URI uri;
    private String password;
    private String certFingerprint;
    private String clientChallengeString;
    private String serverChallengeString;
    private State connectionState = State.DISCONNECTED;
    private Set<Long> syncs = new HashSet<>();

    protected TeddyClient(String uri, String password, String certFingerprint) {

        try {
            this.uri = new URI(uri);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Failed to setURI", e);
            instance.uri = null;
        }
        this.password = password;
        this.certFingerprint = certFingerprint;
    }

    protected TeddyClient(SharedPreferences sharedPref) {
        this(sharedPref.getString(SettingsActivity.KEY_PREF_URI, ""),
                sharedPref.getString(SettingsActivity.KEY_PREF_PASSWORD, ""),
                sharedPref.getString(SettingsActivity.KEY_PREF_CERT_FINGERPRINT, ""));
    }

    protected TeddyClient(String uri, String password) {
        this(uri, password, null);
    }


    public static TeddyClient getInstance(Context context) {
        if (instance == null) {
            Log.d(TAG, "Creating new instance");
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            instance = new TeddyClient(pref);
        }

        return instance;
    }


    public static void updatePreferences(Context context) {
        if (instance != null) {
            Log.i(TAG, "Updating preferences");
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            try {
                instance.uri = new URI(pref.getString(SettingsActivity.KEY_PREF_URI, ""));
            } catch (URISyntaxException e) {
                Log.e(TAG, "Failed to update URI", e);
                instance.uri = null;
            }
            instance.password = pref.getString(SettingsActivity.KEY_PREF_PASSWORD, "");
            instance.certFingerprint = pref.getString(SettingsActivity.KEY_PREF_CERT_FINGERPRINT, "");
            instance.disconnect();
        }
    }


    public void connect() {
        if (this.connectionState != State.DISCONNECTED) {
            return;
        }

        if (this.uri == null) {
            Log.w(TAG, "Uri not configured, not connecting");
            return;
        }

        Log.d(TAG, "Connecting to " + this.uri);
        this.connectionState = State.CONNECTING;
        this.mConnectionHandler = new TeddyConnectionHandler(this.uri, this.certFingerprint, this);
        this.mConnectionHandler.connect();
    }

    protected void onConnect() {
        Log.d(TAG, "Connected");
        this.connectionState = State.CONNECTED;
        sendChallenge();

        for (TeddyCallbackHandler callbackHandler : this.callbackHandlers.values()) {
            callbackHandler.onConnect();
        }
    }

    public void disconnect() {
        Log.d(TAG, "Disconnecting");
        this.mConnectionHandler.close();
    }

    protected void onDisconnect() {
        Log.d(TAG, "Disconnected");
        this.connectionState = State.DISCONNECTED;
        for (TeddyCallbackHandler callbackHandler : this.callbackHandlers.values()) {
            callbackHandler.onDisconnect();
        }

        if (!this.syncs.isEmpty()) {
            // Automatic reconnect after 1s if we are syncing
            new Timer().schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            connect();
                        }
                    }, 1000);
        }
    }

    public void sendChallenge() {
        // Generate random string for client challenge
        this.clientChallengeString = this.generateClientChallenge();
        Log.d(TAG, "Sending client challenge: " + this.clientChallengeString);
        Request request = new Request();
        request.challenge = this.clientChallengeString;
        this.send(request, false);
    }

    protected void onChallenge(String challenge) {
        Log.d(TAG, "Received server challenge: " + challenge);
        this.serverChallengeString = challenge;
        this.sendLogin();
    }

    public void sendLogin() {
        String loginToken = this.getLoginToken();
        Log.d(TAG, "Sending login token: " + loginToken);
        Request request = new Request();
        request.login = loginToken;
        this.send(request, false);
    }

    protected void onLogin() {
        Log.i(TAG, "Logged in!");
        this.connectionState = State.LOGGED_IN;
        // Send queued messages
        Object message;
        while ((message = this.messageQueue.poll()) != null) {
            this.send(message);
        }

        for (TeddyCallbackHandler callbackHandler : this.callbackHandlers.values()) {
            callbackHandler.onLogin();
        }
    }

    public void requestVersion() {
        Log.d(TAG, "Requesting version");
        Request request = new Request();
        request.info = new InfoRequest("version");
        this.send(request);
    }

    protected void onVersion(String version) {
        Log.d(TAG, "Version received: " + version);
        for (TeddyCallbackHandler callbackHandler : this.callbackHandlers.values()) {
            callbackHandler.onVersion(version);
        }
    }

    protected void onPing() {
        Log.d(TAG, "Ping");
    }

    public void requestWindowList() {
        Log.d(TAG, "Requesting window list");
        Request request = new Request();
        request.window = new WindowRequest();
        request.item = new ItemRequest();
        this.send(request);
    }

    protected void onWindowList(List<Window> windowList) {
        Log.d(TAG, "Received window list");
        for (TeddyCallbackHandler callbackHandler : this.callbackHandlers.values()) {
            callbackHandler.onWindowList(windowList);
        }
    }

    public void requestLineList(long windowId) {
        this.requestLineList(windowId, 10, 0);
    }

    public void requestLineList(long windowId, int size) {
        this.requestLineList(windowId, size, 0);
    }

    public void requestLineList(long windowId, int size, int offset) {
        Request request = new Request();
        request.line = new LineRequest();
        request.line.get = new HashMap<>();
        request.line.get.put(windowId, new LineRequest.Get());
        this.send(request);
    }

    protected void onLineList(List<Line> lineList) {
        for (TeddyCallbackHandler callbackHandler : this.callbackHandlers.values()) {
            callbackHandler.onLineList(lineList);
        }
    }

    public void sendInput(long windowId, String message) {
        Request request = new Request();
        request.input = new InputRequest(windowId, message);
        this.send(request);
    }

    public void subscribeLines(long viewId) {
        this.syncs.add(viewId);
        Request request = new Request();
        request.line = new LineRequest();
        request.line.sub_add = new LineRequest.Sub();
        request.line.sub_add.add = new LineRequest.Subscription();
        request.line.sub_add.add.view.add(viewId);
        this.send(request);
    }

    public void unsubscribeLines(long viewId) {
        Request request = new Request();
        request.line = new LineRequest();
        request.line.sub_rm = new LineRequest.Sub();
        request.line.sub_rm.add = new LineRequest.Subscription();
        request.line.sub_rm.add.view.add(viewId);
        this.send(request);
        this.syncs.remove(viewId);
    }

    public void registerCallbackHandler(TeddyCallbackHandler callbackHandler, String handlerKey) {
        this.callbackHandlers.put(handlerKey, callbackHandler);
    }

    public void removeCallBackHandler(String handlerKey) {
        this.callbackHandlers.remove(handlerKey);
    }

    protected void send(Object jsonObject) {
        this.send(jsonObject, true);
    }

    protected void send(Object jsonObject, boolean waitForLogin) {
        switch (this.connectionState) {
            case DISCONNECTED:
                this.connect();
                this.messageQueue.add(jsonObject);
                break;
            case CONNECTING:
            case CONNECTED:
                if (waitForLogin) {
                    // Queue messages to be sent when logged in
                    this.messageQueue.add(jsonObject);
                } else {
                    this.mConnectionHandler.send(jsonObject);
                }
                break;
            case LOGGED_IN:
                this.mConnectionHandler.send(jsonObject);
                break;
            default:
                Log.w(TAG, "Unknown state while sending: " + this.connectionState);
        }
    }

    private String generateClientChallenge() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
            SecretKey clientChallengeKey = keyGenerator.generateKey();
            return Base64.encodeToString(clientChallengeKey.getEncoded(), Base64.NO_PADDING | Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Login challenge generation failed", e);
            return null;
        }
    }

    private String getLoginToken() {
        try {
            // Create secret key from
            String secret = this.serverChallengeString + this.clientChallengeString;
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");

            // Create encoder instance
            Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
            hmacSHA256.init(secret_key);

            // Create token
            byte[] token = hmacSHA256.doFinal(this.password.getBytes());

            // Base64 encode the token
            return Base64.encodeToString(token, Base64.NO_PADDING | Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Login failed", e);
            return null;
        } catch (InvalidKeyException e) {
            Log.e(TAG, "Login failed", e);
            return null;
        }
    }
}

