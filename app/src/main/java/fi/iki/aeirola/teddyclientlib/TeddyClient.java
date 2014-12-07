package fi.iki.aeirola.teddyclientlib;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private static final int TIMEOUT = 15000;

    private static TeddyClient instance;
    private final Queue<Object> messageQueue = new ArrayDeque<>();
    private final Map<String, TeddyCallbackHandler> callbackHandlers = new HashMap<>();
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private TeddyConnectionHandler mConnectionHandler;
    private String uri;
    private String password;
    private String certFingerprint;
    private String clientChallengeString;
    private String serverChallengeString;
    private State connectionState = State.DISCONNECTED;
    private Set<Long> lineSyncs = new HashSet<>();
    private long lastPingReceived;

    protected TeddyClient(String uri, String password, String certFingerprint) {
        this.uri = uri;
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
            instance.uri = pref.getString(SettingsActivity.KEY_PREF_URI, "");
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

    public void reconnect() {
        Log.d(TAG, "Reconnecting to " + this.uri);
        this.mConnectionHandler = new TeddyConnectionHandler(this.uri, this.certFingerprint, this);
        this.mConnectionHandler.connect();
    }

    protected void onConnect() {
        switch (connectionState) {
            case CONNECTING:
                Log.d(TAG, "Connected");
                break;
            case RECONNECTING:
                Log.d(TAG, "Reconnected");
                break;
            default:
                Log.w(TAG, "Invalid state on connect " + connectionState);
        }

        lastPingReceived = System.currentTimeMillis();
        worker.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (connectionState != State.DISCONNECTED && System.currentTimeMillis() - lastPingReceived > TIMEOUT) {
                    // Timed out
                    timeout();
                }
            }
        }, TIMEOUT, TIMEOUT, TimeUnit.MILLISECONDS);

        sendChallenge();
    }

    private void timeout() {
        Log.d(TAG, "Connection ping timeout");
        this.connectionState = State.DISCONNECTING;
        this.mConnectionHandler.close();
    }

    public void disconnect() {
        Log.d(TAG, "Disconnecting");
        this.connectionState = State.DISCONNECTING;
        this.lineSyncs.clear();
        this.messageQueue.clear();
        this.mConnectionHandler.close();
    }

    protected void onDisconnect() {
        Log.d(TAG, "Disconnected");

        if (this.connectionState == State.CONNECTING || this.connectionState == State.RECONNECTING) {
            // Retry connection in half a second
            worker.schedule(new Runnable() {
                @Override
                public void run() {
                    reconnect();
                }
            }, 500, TimeUnit.MILLISECONDS);
            return;
        }

        if (!this.lineSyncs.isEmpty() || !this.messageQueue.isEmpty()) {
            // Reconnect if there is something going on
            this.connectionState = State.RECONNECTING;
            this.reconnect();
            return;
        }

        this.connectionState = State.DISCONNECTED;
        for (TeddyCallbackHandler callbackHandler : this.callbackHandlers.values()) {
            callbackHandler.onDisconnect();
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

        // Update state
        switch (connectionState) {
            case CONNECTING:
                connectionState = State.CONNECTED;
                sendQueuedMessages();

                for (TeddyCallbackHandler callbackHandler : this.callbackHandlers.values()) {
                    callbackHandler.onConnect();
                }
                break;
            case RECONNECTING:
                connectionState = State.RECONNECTED;
                sendQueuedMessages();

                for (long viewId : lineSyncs) {
                    this.subscribeLines(viewId);
                }

                for (TeddyCallbackHandler callbackHandler : this.callbackHandlers.values()) {
                    callbackHandler.onReconnect();
                }
                break;
            default:
                Log.w(TAG, "Invalid connection state during login" + connectionState);
        }
    }

    private void sendQueuedMessages() {
        Object message;
        while ((message = this.messageQueue.poll()) != null) {
            this.send(message);
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
        lastPingReceived = System.currentTimeMillis();
    }

    public void requestWindowList() {
        Log.d(TAG, "Requesting window list");
        Request request = new Request();
        request.window = new WindowRequest();
        request.window.get = new WindowRequest.Get();
        request.item = new ItemRequest();
        this.send(request);
    }

    protected void onWindowList(List<Window> windowList) {
        Log.d(TAG, "Received window list");
        for (TeddyCallbackHandler callbackHandler : this.callbackHandlers.values()) {
            callbackHandler.onWindowList(windowList);
        }
    }

    public void resetWindowActivity(long windowId) {
        Log.d(TAG, "Reseting window activity for " + windowId);
        Request request = new Request();
        request.window = new WindowRequest();
        request.window.dehilight = new ArrayList<>();
        request.window.dehilight.add(windowId);
        this.send(request);
    }

    public void requestLineList(long windowId, int count) {
        LineRequest.Get lineRequest = new LineRequest.Get();
        lineRequest.count = count;
        this.requestLineList(windowId, lineRequest);
    }

    public void requestLineList(long windowId, LineRequest.Get lineRequest) {
        Request request = new Request();
        request.line = new LineRequest();
        request.line.get = new HashMap<>();
        request.line.get.put(windowId, lineRequest);
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
        this.lineSyncs.add(viewId);
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
        this.lineSyncs.remove(viewId);
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
            case RECONNECTING:
                if (waitForLogin) {
                    // Queue messages to be sent when logged in
                    this.messageQueue.add(jsonObject);
                } else {
                    this.mConnectionHandler.send(jsonObject);
                }
                break;
            case CONNECTED:
            case RECONNECTED:
                this.mConnectionHandler.send(jsonObject);
                break;
            case DISCONNECTING:
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

