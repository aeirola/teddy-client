package fi.iki.aeirola.teddyclientlib;

import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import de.tavendo.autobahn.WebSocketConnectionHandler;
import fi.iki.aeirola.teddyclientlib.models.response.CommonResponse;
import fi.iki.aeirola.teddyclientlib.models.response.HDataType;

/**
 * Created by Axel on 26.10.2014.
 */
class TeddyConnectionHandler extends WebSocketConnectionHandler {
    private static final String TAG = TeddyConnectionHandler.class.getName();

    private final TeddyClient teddyClient;
    private final ObjectMapper mObjectMapper;

    public TeddyConnectionHandler(TeddyClient teddyClient, ObjectMapper objectMapper) {
        this.teddyClient = teddyClient;
        this.mObjectMapper = objectMapper;
    }

    @Override
    public void onOpen() {
        Log.v(TAG, "Connected!");
        teddyClient.onConnect();
    }

    @Override
    public void onClose(int code, String reason) {
        Log.v(TAG, "Closed!" + code + reason);
        teddyClient.onDisconnect();
    }

    @Override
    public void onTextMessage(String payload) {
        Log.v(TAG, "Received: " + payload);

        CommonResponse response;
        try {
            response = mObjectMapper.readValue(payload, CommonResponse.class);
        } catch (IOException e) {
            Log.e(TAG, "JSON parsing failed", e);
            return;
        }

        if (response.id != null) {
            switch (response.id) {
                case "_buffer_line_added":
                    teddyClient.onLineList(response);
                    return;
            }
        }

        if (response.challenge != null) {
            teddyClient.onChallenge((response.challenge));
        } else if (response.login != null) {
            if (response.login) {
                teddyClient.onLogin();
            }
        } else if (response.info != null) {
            if (response.info.version != null) {
                teddyClient.onVersion((response.info.version));
            }
        } else if (response.hdata != null) {
            HDataType type = response.getType();
            if (type == null) {
                Log.w(TAG, "HData not identified " + response.hdata);
                return;
            }
            switch (type) {
                case WINDOW:
                    teddyClient.onWindowList(response);
                    break;
                case LINE:
                    teddyClient.onLineList(response);
                    break;
                default:
                    Log.w(TAG, "HData not identified " + type);

            }
        } else if (response.nicklist != null) {
            teddyClient.onNickList(response);
        } else {
            teddyClient.onPing();
        }
    }
}
