package fi.iki.aeirola.teddyclientlib;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.util.Base64;

import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import fi.iki.aeirola.teddyclientlib.models.Line;
import fi.iki.aeirola.teddyclientlib.models.Nick;
import fi.iki.aeirola.teddyclientlib.models.Window;
import fi.iki.aeirola.teddyclientlib.models.request.ChallengeRequest;
import fi.iki.aeirola.teddyclientlib.models.request.HDataRequest;
import fi.iki.aeirola.teddyclientlib.models.request.InfoRequest;
import fi.iki.aeirola.teddyclientlib.models.request.InputRequest;
import fi.iki.aeirola.teddyclientlib.models.request.LoginRequest;
import fi.iki.aeirola.teddyclientlib.models.request.NickListRequest;
import fi.iki.aeirola.teddyclientlib.models.response.CommonResponse;
import fi.iki.aeirola.teddyclientlib.models.response.HDataResponse;
import fi.iki.aeirola.teddyclientlib.models.response.HDataType;
import fi.iki.aeirola.teddyclientlib.models.response.NickListResponse;

/**
 * Created by aeirola on 14.10.2014.
 */
public class TeddyProtocolClient extends WebSocketClient {

    private String password;
    private String clientChallengeString;
    private String serverChallengeString;

    private final Gson gson;
    private final Map<String, TeddyProtocolCallbackHandler> callbackHandlers = new HashMap<String, TeddyProtocolCallbackHandler>();
    private boolean loggedIn = false;
    private final Queue<Object> messageQueue = new ArrayDeque<Object>();

    public TeddyProtocolClient(URI uri, String password) {
        super(uri);
        this.password = password;

        this.gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }



    @Override
    public void onMessage(String message) {
        System.out.println("Messaged!" + message);

        CommonResponse response = this.gson.fromJson(message, CommonResponse.class);
        if (response.challenge != null) {
            this.onChallenge((response.challenge));
        } else if (response.login != null) {
            if (response.login) {
                this.onLogin();
            }
        } else if (response.info != null) {
            if (response.info.version != null) {
                this.onVersion((response.info.version));
            }
        } else if (response.id != null) {
            if (response.id.equals("_buffer_line_added")) {
                this.onLineList(response);
            }
        } else if (response.hdata != null) {
            HDataType type = response.getType();
            if (type == null) {
                return;
            }
            switch (type) {
                case WINDOW:
                    this.onWindowList(response);
                    break;
                case LINE:
                    this.onLineList(response);
                    break;
            }
        } else if (response.nicklist != null) {
            this.onNickList(response);
        } else {
            this.onPing();
        }
    }
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Connected!");
        this.sendChallenge();

        for (TeddyProtocolCallbackHandler callbackHandler : this.callbackHandlers.values()) {
            callbackHandler.onConnect();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Closed!");

        for (TeddyProtocolCallbackHandler callbackHandler : this.callbackHandlers.values()) {
            callbackHandler.onClose();
        }
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("Errored!");
        ex.printStackTrace();
    }

    public void sendChallenge() {
        // Generate random string for client challenge
        this.clientChallengeString = this.generateClientChallenge();
        this.send(new ChallengeRequest(this.clientChallengeString), false);
    }

    public void onChallenge(String challenge) {
        this.serverChallengeString = challenge;
        this.sendLogin();
    }

    public void sendLogin() {
        String loginToken = this.getLoginToken();
        this.send(new LoginRequest(loginToken), false);
    }

    public void onLogin() {
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
        this.send(new InfoRequest("version"));
    }

    public void onVersion(String version) {
        for (TeddyProtocolCallbackHandler callbackHandler : this.callbackHandlers.values()) {
            callbackHandler.onVersion(version);
        }
    }

    public void onPing() {
        System.out.println("Ping");
    }

    public void requestWindowList() {
        this.send(new HDataRequest("buffer:gui_buffers(*)"));
    }

    private void onWindowList(CommonResponse hdata) {
        List<Window> windowList = hdata.toWindowList();
        for (TeddyProtocolCallbackHandler callbackHandler : this.callbackHandlers.values()) {
            callbackHandler.onWindowList(windowList);
        }
    }

    public void requestLineList(Window window) {
        String bufferId = String.valueOf(window.id);
        this.send(new HDataRequest("buffer:0x"+bufferId+"/own_lines/last_line(-10,0)/data"));
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
            super.send(this.gson.toJson(jsonObject));
        }
    }

    private String generateClientChallenge() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
            SecretKey clientChallengeKey = keyGenerator.generateKey();
            return Base64.encodeBytes(clientChallengeKey.getEncoded()).replace("=", "");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
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
            return Base64.encodeBytes(token).replace("=", "");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
    }
}

