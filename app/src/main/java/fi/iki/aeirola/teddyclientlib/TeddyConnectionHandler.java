package fi.iki.aeirola.teddyclientlib;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import fi.iki.aeirola.teddyclientlib.models.response.CommonResponse;
import fi.iki.aeirola.teddyclientlib.models.response.HDataType;

/**
 * Created by Axel on 26.10.2014.
 */
class TeddyConnectionHandler extends WebSocketClient {
    private static final String TAG = TeddyConnectionHandler.class.getName();

    private final TeddyClient teddyClient;
    private final ObjectMapper mObjectMapper = new ObjectMapper();

    public TeddyConnectionHandler(URI uri, String certFingerprint, TeddyClient teddyClient) {
        super(uri);
        this.teddyClient = teddyClient;

        this.mObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.mObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        this.mObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        if (uri != null && uri.getScheme().equals("wss")) {
            trustFingerprint(certFingerprint.replace(":", ""));
        }
    }

    public void trustFingerprint(String fingerprint) {
        try {
            TrustManager[] fingerprintTm = new TrustManager[]{new FingerprintTrustManager(fingerprint)};
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, fingerprintTm, new java.security.SecureRandom());
            this.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sc));
        } catch (Exception e) {
            Log.e(TAG, "Failed to set trust manager", e);
        }
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.v(TAG, "Connected!");
        teddyClient.onConnect();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.v(TAG, "Closed! " + code + " " + reason);
        teddyClient.onDisconnect();
    }

    @Override
    public void onError(Exception ex) {
        Log.e(TAG, "Error in websocket connection", ex);
    }

    @Override
    public void onMessage(String payload) {
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

    public void send(Object jsonObject) {
        try {
            byte[] message = this.mObjectMapper.writeValueAsBytes(jsonObject);
            Log.v(TAG, "Sending: " + new String(message));
            this.send(message);
        } catch (IOException e) {
            Log.e(TAG, "Message sending failed", e);
        }
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