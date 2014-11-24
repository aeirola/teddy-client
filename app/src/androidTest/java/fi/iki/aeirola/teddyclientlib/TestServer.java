package fi.iki.aeirola.teddyclientlib;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_10;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import fi.iki.aeirola.teddyclientlib.models.request.Request;
import fi.iki.aeirola.teddyclientlib.models.response.InfoResponse;
import fi.iki.aeirola.teddyclientlib.models.response.LineResponse;
import fi.iki.aeirola.teddyclientlib.models.response.Response;
import fi.iki.aeirola.teddyclientlib.models.response.WindowResponse;

/**
 * Created by Axel on 21.10.2014.
 */
public class TestServer extends WebSocketServer {
    private static final String TAG = TestServer.class.getName();
    private final ObjectMapper mObjectMapper = new ObjectMapper();

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public TestServer(InetSocketAddress address) throws UnknownHostException {
        super(address, Collections.singletonList((Draft) new Draft_10()));

        this.mObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.mObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        this.mObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        Log.i(TAG, "Listening on " + address.getHostString() + ":" + address.getPort());
    }

    @Override
    public void onWebsocketMessageFragment(WebSocket conn, Framedata frame) {
        Log.v(TAG, String.valueOf(frame));
        super.onWebsocketMessageFragment(conn, frame);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Log.i(TAG, "Client connected");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Log.i(TAG, "Client connection closed: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        Log.i(TAG, "Client sent message: " + message);

        try {
            Request request = this.mObjectMapper.readValue(new ByteBufferBackedInputStream(message), Request.class);
            onMessage(conn, request);
        } catch (IOException e) {
            Log.e(TAG, "JSON parsing failed", e);
            return;
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Log.i(TAG, "Client sent message: " + message);

        try {
            Request request = this.mObjectMapper.readValue(message, Request.class);
            onMessage(conn, request);
        } catch (IOException e) {
            Log.e(TAG, "JSON parsing failed", e);
            return;
        }
    }

    public void onMessage(WebSocket conn, Request request) {
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
        } else if (request.line != null && request.line.get != null) {
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

        if (conn.isOpen()) {
            try {
                conn.send(this.mObjectMapper.writeValueAsString(response));
            } catch (JsonProcessingException e) {
                Log.e(TAG, "JSON sending failed", e);
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        Log.e(TAG, "Exception: ", ex);
    }
}
