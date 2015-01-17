package fi.iki.aeirola.teddyclientlib;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import fi.iki.aeirola.teddyclientlib.models.response.Response;


/**
 * Created by Axel on 26.10.2014.
 */
class TeddyConnectionHandler implements AsyncHttpClient.WebSocketConnectCallback, WebSocket.StringCallback, WebSocket.PongCallback, CompletedCallback, DataCallback {
    private static final String TAG = TeddyConnectionHandler.class.getName();

    private final String uri;
    private final TeddyClient teddyClient;
    private final ObjectMapper mObjectMapper = new ObjectMapper();
    public WebSocket webSocket;

    public TeddyConnectionHandler(String uri, String certFingerprint, TeddyClient teddyClient) {
        this.teddyClient = teddyClient;
        this.uri = uri;

        this.mObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.mObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        this.mObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        if (uri != null && uri.startsWith("wss") &&
                certFingerprint != null && !certFingerprint.isEmpty()) {
            trustFingerprint(certFingerprint.replace(":", ""));
        }
    }

    public void trustFingerprint(String fingerprint) {
        try {
            TrustManager[] fingerprintTm = new TrustManager[]{new FingerprintTrustManager(fingerprint)};
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, fingerprintTm, null);
            // XXX: Not sure why both need to be set here....
            AsyncHttpClient.getDefaultInstance().getSSLSocketMiddleware().setSSLContext(sc);
            AsyncHttpClient.getDefaultInstance().getSSLSocketMiddleware().setTrustManagers(fingerprintTm);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set trust manager", e);
        }
    }

    public void connect() {
        AsyncHttpClient.getDefaultInstance().websocket(this.uri, "teddy-nu", this);
    }

    public void close() {
        if (this.webSocket != null && this.webSocket.isOpen())
            this.webSocket.close();
    }

    public void send(Object jsonObject) {
        try {
            String message = this.mObjectMapper.writeValueAsString(jsonObject);
            Log.v(TAG, "Sending: " + message);
            this.webSocket.send(message);
        } catch (IOException e) {
            Log.e(TAG, "Message sending failed", e);
        }
    }

    @Override
    public void onCompleted(Exception ex, WebSocket webSocket) {
        if (ex != null) {
            this.onCompleted(ex);
        } else {
            this.webSocket = webSocket;
            if (webSocket != null) {
                webSocket.setStringCallback(this);
                webSocket.setDataCallback(this);
                webSocket.setPongCallback(this);
                webSocket.setClosedCallback(this);
            }

            Log.v(TAG, "Connected!");
            teddyClient.onConnect();
        }
    }

    @Override
    public void onCompleted(Exception ex) {
        if (ex != null) {
            Log.w(TAG, ex.toString());
        } else {
            Log.i(TAG, "Connection closed");
        }

        this.webSocket = null;
        teddyClient.onDisconnect();
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

        this.teddyClient.onMessage(response);
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

        this.teddyClient.onMessage(response);
    }

    @Override
    public void onPongReceived(String s) {
        Log.v(TAG, "Pong received " + s);
    }

}


class FingerprintTrustManager implements X509TrustManager {
    final static private String TAG = FingerprintTrustManager.class.getName();
    final protected char[] hexArray = "0123456789ABCDEF".toCharArray();

    final private String fingerprint;

    public FingerprintTrustManager(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    @Override
    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return new java.security.cert.X509Certificate[]{};
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        throw new CertificateException("Client certificates are not supported");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain == null || chain.length < 1) {
            throw new IllegalArgumentException("Invalid certificate length");
        }

        X509Certificate cert = chain[0];
        Log.v(TAG, cert.getIssuerDN().getName());

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(cert.getEncoded());
            String fingerprint = bytesToHex(md.digest());
            Log.v(TAG, "Server fingerprint: " + fingerprint);
            Log.v(TAG, "Stored fingerprint: " + this.fingerprint);
            if (!fingerprint.equals(this.fingerprint)) {
                throw new CertificateException("Fingerprint missmatch: " + fingerprint + " != " + this.fingerprint);
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "failed to calculate fingerprint", e);
            throw new CertificateException(e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}