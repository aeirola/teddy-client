package fi.iki.aeirola.teddyclientlib;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketConnectionHandler;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketOptions;
import fi.iki.aeirola.teddyclient.SettingsActivity;
import fi.iki.aeirola.teddyclientlib.models.Line;
import fi.iki.aeirola.teddyclientlib.models.Nick;
import fi.iki.aeirola.teddyclientlib.models.Window;
import fi.iki.aeirola.teddyclientlib.models.request.ChallengeRequest;
import fi.iki.aeirola.teddyclientlib.models.request.DesyncRequest;
import fi.iki.aeirola.teddyclientlib.models.request.HDataRequest;
import fi.iki.aeirola.teddyclientlib.models.request.InfoRequest;
import fi.iki.aeirola.teddyclientlib.models.request.InputRequest;
import fi.iki.aeirola.teddyclientlib.models.request.LoginRequest;
import fi.iki.aeirola.teddyclientlib.models.request.NickListRequest;
import fi.iki.aeirola.teddyclientlib.models.request.SyncRequest;
import fi.iki.aeirola.teddyclientlib.models.response.CommonResponse;

/**
 * Created by aeirola on 14.10.2014.
 */
public class TeddyClient {
    private static final String TAG = "TeddyProtocolClient";
    private static TeddyClient instance;
    private final WebSocketConnectionHandler mConnectionHandler;
    private final WebSocket mConnection;
    private final WebSocketOptions mConnectionOptions;
    private final ObjectMapper mObjectMapper = new ObjectMapper();
    private final Queue<Object> messageQueue = new ArrayDeque<>();
    private final Map<String, TeddyCallbackHandler> callbackHandlers = new HashMap<>();
    private String uri;
    private String password;
    private String clientChallengeString;
    private String serverChallengeString;
    private State connectionState = State.DISCONNECTED;
    private SyncState syncState = SyncState.DISABLED;

    protected TeddyClient(String uri, String password) {
        this.mConnection = new WebSocketConnection();
        this.mConnectionOptions = new WebSocketOptions();
        this.mConnectionOptions.setReceiveTextMessagesRaw(false);
        this.mConnectionOptions.setValidateIncomingUtf8(true);
        this.mConnectionOptions.setMaskClientFrames(true);
        this.mConnectionOptions.setTcpNoDelay(true);

        this.mObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.mObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        this.mObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        this.mConnectionHandler = new TeddyEventHandler(this, mObjectMapper);

        this.uri = uri;
        this.password = password;
    }

    protected TeddyClient(SharedPreferences sharedPref) {
        this(sharedPref.getString(SettingsActivity.KEY_PREF_URI, ""),
                sharedPref.getString(SettingsActivity.KEY_PREF_PASSWORD, ""));
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
            instance.disconnect();
        }
    }


    public void connect() {
        if (this.connectionState == State.CONNECTING || this.mConnection.isConnected()) {
            return;
        }

        if (this.uri == null || this.uri.isEmpty()) {
            Log.w(TAG, "Uri not configured, not connecting");
            return;
        }

        try {
            Log.d(TAG, "Connecting");
            this.connectionState = State.CONNECTING;
            this.mConnection.connect(this.uri, this.mConnectionHandler, this.mConnectionOptions);
        } catch (WebSocketException e) {
            Log.e(TAG, "Connection failed", e);
        }
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
        if (!this.mConnection.isConnected()) {
            Log.d(TAG, "Already disconnected");
        } else {
            Log.d(TAG, "Disconnecting");
            this.mConnection.disconnect();
        }
    }

    protected void onDisconnect() {
        Log.d(TAG, "Disconnected");
        this.connectionState = State.DISCONNECTED;
        for (TeddyCallbackHandler callbackHandler : this.callbackHandlers.values()) {
            callbackHandler.onDisconnect();
        }

        if (syncState == SyncState.ENABLED) {
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
        this.send(new ChallengeRequest(this.clientChallengeString), false);
    }

    protected void onChallenge(String challenge) {
        Log.d(TAG, "Received server challenge: " + challenge);
        this.serverChallengeString = challenge;
        this.sendLogin();
    }

    public void sendLogin() {
        String loginToken = this.getLoginToken();
        Log.d(TAG, "Sending login token: " + loginToken);
        this.send(new LoginRequest(loginToken), false);
    }

    protected void onLogin() {
        Log.i(TAG, "Logged in!");
        this.connectionState = State.LOGGED_IN;
        // Send queued messages
        Object message;
        while ((message = this.messageQueue.poll()) != null) {
            this.send(message);
        }

        // Return to previous sync state
        if (syncState == SyncState.ENABLED) {
            this.enableSync();
        }

        for (TeddyCallbackHandler callbackHandler : this.callbackHandlers.values()) {
            callbackHandler.onLogin();
        }
    }

    public void requestVersion() {
        Log.d(TAG, "Requesting version");
        this.send(new InfoRequest("version"));
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
        this.send(new HDataRequest("buffer:gui_buffers(*)"));
    }

    protected void onWindowList(CommonResponse hdata) {
        Log.d(TAG, "Received window list");
        List<Window> windowList = hdata.toWindowList();
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
        this.send(new HDataRequest("buffer:0x" + windowId + "/own_lines/last_line(-" + size + "," + offset + ")/data"));
    }

    protected void onLineList(CommonResponse hdata) {
        List<Line> lineList = hdata.toLineList();
        for (TeddyCallbackHandler callbackHandler : this.callbackHandlers.values()) {
            callbackHandler.onLineList(lineList);
        }
    }

    public void requestNickList(Window window) {
        this.send(new NickListRequest(window.fullName));
    }

    protected void onNickList(CommonResponse nicklist) {
        List<Nick> nickList = nicklist.toNickList();
        for (TeddyCallbackHandler callbackHandler : this.callbackHandlers.values()) {
            callbackHandler.onNickList(nickList);
        }
    }

    public void sendInput(String windowName, String message) {
        InputRequest input = new InputRequest(windowName, message);
        this.send(input);
    }

    public void enableSync() {
        this.syncState = SyncState.ENABLED;
        this.send(new SyncRequest());
    }

    public void disableSync() {
        this.send(new DesyncRequest());
        this.syncState = SyncState.DISABLED;
    }

    public void sendQuit() {
        this.send(new InputRequest("core.weechat", "/quit"));
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
                    this.sendDirect(jsonObject);
                }
                break;
            case LOGGED_IN:
                this.sendDirect(jsonObject);
                break;
            default:
                Log.w(TAG, "Unknown state while sending: " + this.connectionState);
        }
    }

    private void sendDirect(Object jsonObject) {
        try {
            byte[] message = this.mObjectMapper.writeValueAsBytes(jsonObject);
            Log.v(TAG, "Sending: " + new String(message));
            this.mConnection.sendRawTextMessage(message);
        } catch (IOException e) {
            Log.e(TAG, "Message sending failed", e);
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

