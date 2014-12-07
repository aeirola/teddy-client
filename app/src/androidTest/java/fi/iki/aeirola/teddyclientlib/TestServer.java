package fi.iki.aeirola.teddyclientlib;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;

import fi.iki.aeirola.teddyclientlib.models.request.Request;
import fi.iki.aeirola.teddyclientlib.models.response.InfoResponse;
import fi.iki.aeirola.teddyclientlib.models.response.LineResponse;
import fi.iki.aeirola.teddyclientlib.models.response.Response;
import fi.iki.aeirola.teddyclientlib.models.response.WindowResponse;

/**
 * Created by Axel on 21.10.2014.
 */
public class TestServer {
    private static final String TAG = TestServer.class.getName();
    private final AsyncHttpServer server;
    private final InetSocketAddress address;

    public TestServer(InetSocketAddress address) {
        this.address = address;

        TestServerCallbackHandler callbackHandler = new TestServerCallbackHandler();

        this.server = new AsyncHttpServer();
        this.server.setErrorCallback(callbackHandler);
        this.server.websocket("/teddy", "teddy-nu", callbackHandler);
    }

    public void start() {
        Log.i(TAG, "Listening on " + address.getHostName() + ":" + address.getPort());
        this.server.listen(address.getPort());
    }

    public void stop() {
        this.server.stop();
    }
}

class TestServerCallbackHandler implements AsyncHttpServer.WebSocketRequestCallback, WebSocket.StringCallback, WebSocket.PongCallback, CompletedCallback {
    private static final String TAG = TestServerCallbackHandler.class.getName();
    private final ObjectMapper mObjectMapper = new ObjectMapper();

    {
        this.mObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.mObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        this.mObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private WebSocket webSocket;

    @Override
    public void onConnected(final WebSocket webSocket, AsyncHttpServerRequest request) {
        Log.i(TAG, "Client connected at " + request.getPath());
        this.webSocket = webSocket;
        webSocket.setStringCallback(this);
        webSocket.setPongCallback(this);
        webSocket.setClosedCallback(this);
    }

    @Override
    public void onStringAvailable(String s) {
        Log.i(TAG, "Client sent message: " + s);

        try {
            Request request = this.mObjectMapper.readValue(s, Request.class);
            onMessage(request);
        } catch (IOException e) {
            Log.e(TAG, "JSON parsing failed", e);
            return;
        }
    }

    public void onMessage(Request request) {
        Response response = new Response();
        if (request.challenge != null) {
            response.challenge = "test-server-challenge";
        } else if (request.login != null) {
            response.login = true;
        } else if (request.info != null) {
            response.info = new InfoResponse();
            if (request.info.name.equals("version")) {
                response.info.version = "test-server-1.0";
            } else {
                Log.w(TAG, "Unknown info request: " + request.info.name);
            }
        } else if (request.window != null) {
            response.window = new WindowResponse();
            if (request.window.get != null) {
                response.window.get = new ArrayList<>();
                WindowResponse.WindowData windowData = new WindowResponse.WindowData();
                windowData.id = 1L;
                windowData.view = 1000L;
                windowData.name = "(status)";
                response.window.get.add(windowData);
            }
        } else if (request.line != null) {
            response.line = new LineResponse();
            if (request.line.get != null) {
                response.line.get = new HashMap<>();
                response.line.get.put(1000L, new ArrayList<LineResponse.LineData>());
                LineResponse.LineData lineData = new LineResponse.LineData();
                lineData.text = "22:00 <test_user> hello there!";
                response.line.get.get(1000L).add(lineData);
            } else if (request.line.sub_add != null) {
                return;
            } else if (request.line.sub_rm != null) {
                return;
            }
        } else if (request.input != null) {
            response.lineAdded = new HashMap<>();
            response.lineAdded.put(1000L, new ArrayList<LineResponse.LineData>());
            LineResponse.LineData lineData = new LineResponse.LineData();
            lineData.text = "22:00 <test_user> " + request.input.data;
            response.lineAdded.get(1000L).add(lineData);
        } else {
            Log.w(TAG, "Unknown request");
        }

        if (webSocket != null && webSocket.isOpen()) {
            try {
                webSocket.send(this.mObjectMapper.writeValueAsString(response));
            } catch (JsonProcessingException e) {
                Log.e(TAG, "JSON sending failed", e);
            }
        }
    }

    @Override
    public void onPongReceived(String s) {
        Log.d(TAG, "Pong received " + s);
    }

    @Override
    public void onCompleted(Exception ex) {
        Log.w(TAG, "Closed " + ex.getMessage());
        this.webSocket = null;
    }
}
