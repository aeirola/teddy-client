package fi.iki.aeirola.teddyclientlib;

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

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketConnectionHandler;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketOptions;
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
import fi.iki.aeirola.teddyclientlib.models.response.HDataType;

/**
 * Created by aeirola on 14.10.2014.
 */
public class TeddyProtocolClient {
    private static final String TAG = "TeddyProtocolClient";
    private final WebSocketConnectionHandler mConnectionHandler = new WebSocketConnectionHandler() {

        @Override
        public void onOpen() {
            Log.d(TAG, "Connected!");
            TeddyProtocolClient.this.sendChallenge();

            for (TeddyProtocolCallbackHandler callbackHandler : TeddyProtocolClient.this.callbackHandlers.values()) {
                callbackHandler.onConnect();
            }
        }

        @Override
        public void onClose(int code, String reason) {
            Log.d(TAG, "Closed!" + code + reason);

            for (TeddyProtocolCallbackHandler callbackHandler : TeddyProtocolClient.this.callbackHandlers.values()) {
                callbackHandler.onClose();
            }
        }

        @Override
        public void onTextMessage(String payload) {
            Log.v(TAG, "Received: " + payload);

            CommonResponse response = null;
            try {
                response = TeddyProtocolClient.this.mObjectMapper.readValue(payload, CommonResponse.class);
            } catch (IOException e) {
                Log.e(TAG, "JSON parsing failed", e);
                return;
            }

            if (response.id != null) {
                switch (response.id) {
                    case "_buffer_line_added":
                        TeddyProtocolClient.this.onLineList(response);
                        return;
                }
            }

            if (response.challenge != null) {
                TeddyProtocolClient.this.onChallenge((response.challenge));
            } else if (response.login != null) {
                if (response.login) {
                    TeddyProtocolClient.this.onLogin();
                }
            } else if (response.info != null) {
                if (response.info.version != null) {
                    TeddyProtocolClient.this.onVersion((response.info.version));
                }
            } else if (response.hdata != null) {
                HDataType type = response.getType();
                if (type == null) {
                    Log.w(TAG, "HData not identified " + response.hdata);
                    return;
                }
                switch (type) {
                    case WINDOW:
                        TeddyProtocolClient.this.onWindowList(response);
                        break;
                    case LINE:
                        TeddyProtocolClient.this.onLineList(response);
                        break;
                    default:
                        Log.w(TAG, "HData not identified " + type);

                }
            } else if (response.nicklist != null) {
                TeddyProtocolClient.this.onNickList(response);
            } else {
                TeddyProtocolClient.this.onPing();
            }
        }
    };
    private final WebSocket mConnection;
    private final WebSocketOptions mConnectionOptions;
    private final ObjectMapper mObjectMapper = new ObjectMapper();
    private final Queue<Object> messageQueue = new ArrayDeque<Object>();
    private final Map<String, TeddyProtocolCallbackHandler> callbackHandlers = new HashMap<String, TeddyProtocolCallbackHandler>();
    private String uri;
    private String password;
    private String clientChallengeString;
    private String serverChallengeString;
    private boolean loggedIn = false;

    public TeddyProtocolClient(String uri, String password) {
        this.mConnection = new WebSocketConnection();
        this.mConnectionOptions = new WebSocketOptions();
        this.mConnectionOptions.setReceiveTextMessagesRaw(false);
        this.mConnectionOptions.setValidateIncomingUtf8(true);
        this.mConnectionOptions.setMaskClientFrames(true);
        this.mConnectionOptions.setTcpNoDelay(true);

        this.mObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.mObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        this.mObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        this.uri = uri;
        this.password = password;
    }

    public void connect() {
        try {
            Log.d(TAG, "Connecting");
            this.mConnection.connect(this.uri, this.mConnectionHandler, this.mConnectionOptions);
        } catch (WebSocketException e) {
            Log.e(TAG, "Connection failed", e);
        }
    }

    public void disconnect() {
        Log.d(TAG, "Disonnecting");
        this.mConnection.disconnect();
    }

    public void sendChallenge() {
        // Generate random string for client challenge
        this.clientChallengeString = this.generateClientChallenge();
        Log.d(TAG, "Sending client challenge: " + this.clientChallengeString);
        this.send(new ChallengeRequest(this.clientChallengeString), false);
    }

    public void onChallenge(String challenge) {
        Log.d(TAG, "Received server challenge: " + challenge);
        this.serverChallengeString = challenge;
        this.sendLogin();
    }

    public void sendLogin() {
        String loginToken = this.getLoginToken();
        Log.d(TAG, "Sending login token: " + loginToken);
        this.send(new LoginRequest(loginToken), false);
    }

    public void onLogin() {
        Log.i(TAG, "Logged in!");
        this.loggedIn = true;
        // Send queued messages
        Object message;
        while ((message = this.messageQueue.poll()) != null) {
            this.send(message);
        }

        for (TeddyProtocolCallbackHandler callbackHandler : this.callbackHandlers.values()) {
            callbackHandler.onLogin();
        }
    }

    public void requestVersion() {
        Log.d(TAG, "Require version");
        this.send(new InfoRequest("version"));
    }

    public void onVersion(String version) {
        Log.d(TAG, "Version received: " + version);
        for (TeddyProtocolCallbackHandler callbackHandler : this.callbackHandlers.values()) {
            callbackHandler.onVersion(version);
        }
    }

    public void onPing() {
        Log.d(TAG, "Ping");
    }

    public void requestWindowList() {
        Log.d(TAG, "Requesting window list");
        this.send(new HDataRequest("buffer:gui_buffers(*)"));
    }

    private void onWindowList(CommonResponse hdata) {
        Log.d(TAG, "Received window list");
        List<Window> windowList = hdata.toWindowList();
        for (TeddyProtocolCallbackHandler callbackHandler : this.callbackHandlers.values()) {
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

    private void onLineList(CommonResponse hdata) {
        List<Line> lineList = hdata.toLineList();
        for (TeddyProtocolCallbackHandler callbackHandler : this.callbackHandlers.values()) {
            callbackHandler.onLineList(lineList);
        }
    }

    public void requestNickList(Window window) {
        this.send(new NickListRequest(window.fullName));
    }

    private void onNickList(CommonResponse nicklist) {
        List<Nick> nickList = nicklist.toNickList();
        for (TeddyProtocolCallbackHandler callbackHandler : this.callbackHandlers.values()) {
            callbackHandler.onNickList(nickList);
        }
    }

    public void sendInput(String windowName, String message) {
        InputRequest input = new InputRequest(windowName, message);
        this.send(input);
    }

    public void enableSync() {
        this.send(new SyncRequest());
    }

    public void disableSync() {
        this.send(new DesyncRequest());
    }


    public void sendQuit() {
        this.send(new InputRequest("core.weechat", "/quit"));
    }

    public void registerCallbackHandler(TeddyProtocolCallbackHandler callbackHandler, String handlerKey) {
        this.callbackHandlers.put(handlerKey, callbackHandler);
    }

    public void removeCallBackHandler(String handlerKey) {
        this.callbackHandlers.remove(handlerKey);
    }

    private void send(Object jsonObject) {
        this.send(jsonObject, true);
    }

    private void send(Object jsonObject, boolean waitForLogin) {
        if (waitForLogin && !this.loggedIn) {
            // Queue messages to be sent when logged in
            this.messageQueue.add(jsonObject);
        } else {
            try {
                byte[] message = this.mObjectMapper.writeValueAsBytes(jsonObject);
                Log.v(TAG, "Sending: " + new String(message));
                this.mConnection.sendRawTextMessage(message);
            } catch (IOException e) {
                Log.e(TAG, "Message sending failed", e);
            }
        }
    }

    private String generateClientChallenge() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
            SecretKey clientChallengeKey = keyGenerator.generateKey();
            return Base64.encodeToString(clientChallengeKey.getEncoded(), Base64.NO_PADDING | Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Login challenge generatin failed", e);
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

