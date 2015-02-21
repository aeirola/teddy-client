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
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.iki.aeirola.teddyclientlib.models.Window;
import fi.iki.aeirola.teddyclientlib.models.request.Request;
import fi.iki.aeirola.teddyclientlib.models.response.InfoResponse;
import fi.iki.aeirola.teddyclientlib.models.response.ItemResponse;
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
    private final TestServerCallbackHandler mCallbackHandler;

    public TestServer(InetSocketAddress address) {
        this.address = address;

        mCallbackHandler = new TestServerCallbackHandler();
        mCallbackHandler.setFixture(new TestServerFixture());

        this.server = new AsyncHttpServer();
        this.server.setErrorCallback(mCallbackHandler);
        this.server.websocket("/teddy", "teddy-nu", mCallbackHandler);
    }

    public void start() {
        Log.i(TAG, "Listening on " + address.getHostName() + ":" + address.getPort());
        this.server.listen(address.getPort());
    }

    public void stop() {
        this.server.stop();
    }

    public void setFixture(TestServerFixture fixture) {
        mCallbackHandler.setFixture(fixture);
    }
}

class TestServerFixture {

    String[][] windows = new String[][]{
            {"1", "(status)", "passive"},
            {"2", "#channel", "active"},
            {"3", "query", "hilight"},
    };

    String[][] lines = new String[][]{
            {
                    "Day changed to 31 Jan 2015",
                    "01:46 ee screen_away: Set away",
                    "01:46 ee You have been marked as being away"
            }, {
            "22:00 <nick1> Hello there"
    }, {
            "21:00 <query> Hello there!"
    },
    };

    public List<WindowResponse.WindowData> getWindows() {
        List<WindowResponse.WindowData> responses = new ArrayList<>(windows.length);
        long i = 0;
        for (String[] window : windows) {
            i++;

            WindowResponse.WindowData windowData = new WindowResponse.WindowData();
            windowData.id = i;
            windowData.view = i + 1000L;
            windowData.refnum = Integer.valueOf(window[0]);
            windowData.name = window[1];
            windowData.dataLevel = Window.Activity.valueOf(window[2].toUpperCase()).ordinal();

            responses.add(windowData);
        }
        return responses;
    }

    public List<ItemResponse.ItemData> getItems() {
        List<ItemResponse.ItemData> responses = new ArrayList<>(windows.length);
        long i = 0;
        for (String[] window : windows) {
            i++;

            ItemResponse.ItemData itemData = new ItemResponse.ItemData();
            itemData.window = i;
            itemData.id = i + 1000L;
            itemData.visibleName = window[1];

            responses.add(itemData);
        }
        return responses;
    }

    public Map<Long, List<LineResponse.LineData>> getLines(Set<Long> viewIds) {
        Map<Long, List<LineResponse.LineData>> responses = new HashMap<>(windows.length);
        long i = 1000L;
        for (String[] windowLines : lines) {
            i++;

            if (!viewIds.contains(i)) {
                continue;
            }
            List<LineResponse.LineData> lines = new ArrayList<>();
            for (String windowLine : windowLines) {
                LineResponse.LineData lineData = new LineResponse.LineData();
                lineData.text = windowLine;
                lines.add(lineData);
            }

            responses.put(i, lines);
        }
        return responses;
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
    private TestServerFixture fixture;

    public void setFixture(TestServerFixture fixture) {
        this.fixture = fixture;
    }

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
        response.id = request.id;
        if (request.challenge != null) {
            response.challenge = "test-server-challenge";
        }
        if (request.login != null) {
            response.login = true;
        }
        if (request.info != null) {
            response.info = new InfoResponse();
            if (request.info.name.equals("version")) {
                response.info.version = "test-server-1.0";
            } else {
                Log.w(TAG, "Unknown info request: " + request.info.name);
            }
        }

        if (request.window != null) {
            response.window = new WindowResponse();
            if (request.window.get != null) {
                response.window.get = fixture.getWindows();
            }
        }

        if (request.item != null) {
            response.item = new ItemResponse();
            if (request.item.get != null) {
                response.item.get = fixture.getItems();
            }
        }

        if (request.line != null) {
            response.line = new LineResponse();
            if (request.line.get != null) {
                response.line.get = fixture.getLines(request.line.get.keySet());
            } else if (request.line.sub_add != null) {
                return;
            } else if (request.line.sub_rm != null) {
                return;
            }
        }

        if (request.input != null) {
            response.lineAdded = new HashMap<>();
            response.lineAdded.put(1000L, new ArrayList<LineResponse.LineData>());
            LineResponse.LineData lineData = new LineResponse.LineData();
            lineData.text = "22:00 <test_user> " + request.input.data;
            response.lineAdded.get(1000L).add(lineData);
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
