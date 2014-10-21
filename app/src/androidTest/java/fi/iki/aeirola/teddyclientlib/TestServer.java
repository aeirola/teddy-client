package fi.iki.aeirola.teddyclientlib;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import fi.iki.aeirola.teddyclientlib.models.response.CommonResponse;
import fi.iki.aeirola.teddyclientlib.models.response.HDataResponse;
import fi.iki.aeirola.teddyclientlib.models.response.InfoResponse;
import fi.iki.aeirola.teddyclientlib.models.response.NickListResponse;

/**
 * Created by Axel on 21.10.2014.
 */
public class TestServer extends WebSocketServer {
    private static final String TAG = "TestServer";
    private final ObjectMapper mObjectMapper = new ObjectMapper();

    public TestServer(InetSocketAddress address) throws UnknownHostException {
        super(address);

        this.mObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.mObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        this.mObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        Log.i(TAG, "Listening on " + address.getHostString() + ":" + address.getPort());
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
    public void onMessage(WebSocket conn, String message) {
        Log.i(TAG, "Client sent message: " + message);
        CommonRequest request;

        try {
            request = this.mObjectMapper.readValue(message, CommonRequest.class);
        } catch (IOException e) {
            Log.e(TAG, "JSON parsing failed", e);
            return;
        }

        CommonResponse response = new CommonResponse();
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
        } else if (request.hdata != null) {
            response.hdata = new ArrayList<HDataResponse>();
            if (request.hdata.path.equals("buffer:gui_buffers(*)")) {
                // Windows
                HDataResponse hdata = new HDataResponse();
                hdata.pointers = new long[]{1L, 2L};
                hdata.shortName = "(status)";
                hdata.fullName = "empty.1";
                response.hdata.add(hdata);
            } else if (request.hdata.path.contains("buffer:0x")) {
                // Messages
                HDataResponse hdata = new HDataResponse();
                hdata.highlight = 0;
                hdata.buffer = 1L;
                hdata.date = "";
                hdata.fromNick = "test_user";
                hdata.message = "hello there!";
                response.hdata.add(hdata);
            }
        } else if (request.nicklist != null) {
            response.nicklist = new ArrayList<NickListResponse>();
            NickListResponse nick = new NickListResponse();
            nick.name = "test_user";

            response.nicklist.add(nick);
        } else {
            Log.w(TAG, "Unknown request");
        }

        if (response != null && conn.isOpen()) {
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
