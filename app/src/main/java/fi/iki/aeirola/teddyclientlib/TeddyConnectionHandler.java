package fi.iki.aeirola.teddyclientlib;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.TransformFuture;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.spdy.SpdyMiddleware;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Comparator;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import fi.iki.aeirola.teddyclientlib.models.response.Response;
import fi.iki.aeirola.teddyclientlib.utils.SSLCertHelper;


/**
 * Handler for connections to the Teddy server instance. Wraps around the Android async WebSocket client.
 *
 * Created by Axel on 26.10.2014.
 */
class TeddyConnectionHandler implements AsyncHttpClient.WebSocketConnectCallback, WebSocket.StringCallback, CompletedCallback, DataCallback {
    private static final String TAG = TeddyConnectionHandler.class.getName();

    private final String url;
    private final ObjectMapper mObjectMapper = new ObjectMapper();
    private final AsyncHttpClient asyncClient;
    public WebSocket webSocket;
    private TeddyClient teddyClient;

    public TeddyConnectionHandler(String url, String cert, TeddyClient teddyClient) {
        this.teddyClient = teddyClient;
        this.url = url;
        // The teddy server seems to prefer ipv4, so we'll do the same.
        this.asyncClient = new AsyncHttpClient(new Ipv4AsyncServer());

        this.mObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.mObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        this.mObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        if (url != null && url.startsWith("wss") &&
                cert != null && !cert.isEmpty()) {
            trustCert(cert.replace(":", ""));
        }
    }

    public void trustCert(String cert) {
        try {
            TrustManager[] trustManagers = SSLCertHelper.initializeTrustManager(cert);

            // Create an SSLContext that uses our TrustManager
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustManagers, null);

            SpdyMiddleware middleware = this.asyncClient.getSSLSocketMiddleware();
            middleware.setSSLContext(context);
            middleware.setTrustManagers(trustManagers);

        } catch (Exception e) {
            Log.e(TAG, "Failed to set trust manager", e);
        }
    }

    public void connect() {
        this.asyncClient.websocket(this.url, "teddy-nu", this);
    }

    public void close() {
        teddyClient = null;

        if (this.webSocket != null) {
            this.webSocket.close();
            this.webSocket = null;
        }

    }

    public void send(Object jsonObject) {
        try {
            String message = this.mObjectMapper.writeValueAsString(jsonObject);
            Log.v(TAG, "Sending: " + message);
            if (this.webSocket == null) {
                Log.w(TAG, "Not connected, could not send message!");
            } else {
                this.webSocket.send(message);
            }
        } catch (IOException e) {
            Log.e(TAG, "Message sending failed", e);
        }
    }

    @Override
    public void onCompleted(Exception ex, WebSocket webSocket) {
        /*
         * Called when connection is opened, unless exception is present
         */
        if (ex != null) {
            this.onCompleted(ex);
            return;
        }

        if (webSocket == null) {
            this.onCompleted(null);
            return;
        }

        if (teddyClient == null) {
            this.close();
            return;
        }

        this.webSocket = webSocket;
        webSocket.setStringCallback(this);
        webSocket.setDataCallback(this);
        webSocket.setClosedCallback(this);

        Log.v(TAG, "Connected!");
        teddyClient.onConnect();
    }

    @Override
    public void onCompleted(Exception ex) {
        /*
         * Called when connection is closed
         */
        if (ex != null) {
            Log.w(TAG, ex.toString());
        } else {
            Log.i(TAG, "Connection closed");
        }

        if (this.webSocket != null) {
            this.webSocket.close();
            this.webSocket = null;
        }

        if (teddyClient != null) {
            teddyClient.onDisconnect();
            teddyClient = null;
        }
    }

    @Override
    public void onStringAvailable(String s) {
        Log.v(TAG, "Received: " + s);

        if (s.equals("{}")) {
            teddyClient.onPing();
            return;
        }

        Response response;
        try {
            response = mObjectMapper.readValue(s, Response.class);
        } catch (IOException e) {
            Log.e(TAG, "JSON parsing failed", e);
            return;
        }


        if (teddyClient != null) {
            teddyClient.onMessage(response);
        }
    }

    @Override
    public void onDataAvailable(DataEmitter emitter, ByteBufferList byteBufferList) {
        Log.v(TAG, "Received bytes");
        Response response;
        try {
            response = mObjectMapper.readValue(byteBufferList.getAllByteArray(), Response.class);
        } catch (IOException e) {
            Log.e(TAG, "JSON parsing failed", e);
            return;
        } finally {
            byteBufferList.recycle();
        }

        if (teddyClient != null) {
            teddyClient.onMessage(response);
        }
    }

    /**
     * Modified version of @AsyncServer that prefers IPv4 addresses over IPv6 addresses.
     */
    private class Ipv4AsyncServer extends AsyncServer {
        /**
         * Resolves all addresses for host, sorted by address version.
         *
         * @param host Hostname to resolve
         * @return Array of resolved addresses, with IPv4 addresses on top
         */
        @Override
        public Future<InetAddress[]> getAllByName(final String host) {
            return super.getAllByName(host).then(new TransformFuture<InetAddress[], InetAddress[]>() {
                @Override
                protected void transform(InetAddress[] result) throws Exception {
                    Arrays.sort(result, new Comparator<InetAddress>() {
                        @Override
                        public int compare(InetAddress lhs, InetAddress rhs) {
                            if (lhs instanceof Inet4Address && rhs instanceof Inet6Address) {
                                return -1;
                            } else if (lhs instanceof Inet6Address && rhs instanceof Inet4Address) {
                                return 1;
                            } else {
                                return 0;
                            }
                        }
                    });
                    setComplete(result);
                }
            });
        }
    }
}
