package fi.iki.aeirola.teddyclientlib.utils;

import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Formatter;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Created by Axel on 12.2.2015.
 */
public class SSLCertHelper {
    private static final String TAG = SSLCertHelper.class.getName();

    public static String getCertFingerprint(String cert) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] certBytes = Base64.decode(cert, Base64.DEFAULT);
            byte[] sha1Bytes = md.digest(certBytes);

            Formatter formatter = new Formatter();
            for (byte b : sha1Bytes) {
                formatter.format("%02X:", b);
            }
            String fingerprint = formatter.toString();
            return fingerprint.substring(0, fingerprint.length() - 1);
        } catch (Exception e) {
            Log.w(TAG, e.toString());
            return null;
        }
    }

    public static TrustManager[] initializeTrustManager(String cert) throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        InputStream caInput = new ByteArrayInputStream(Base64.decode(cert, Base64.DEFAULT));

        return initializeTrustManager(caInput);
    }

    private static TrustManager[] initializeTrustManager(InputStream caInput) throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        // Load CAs from an InputStream
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        Certificate ca;
        try {
            ca = cf.generateCertificate(caInput);
        } finally {
            caInput.close();
        }

        // Create a KeyStore containing our trusted CAs
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        // Create a TrustManager that trusts the CAs in our KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

        return tmf.getTrustManagers();
    }

    public static String getCert(URI url) {
        // Inspired by https://code.google.com/p/java-use-examples/source/browse/trunk/src/com/aw/ad/util/InstallCert.java

        CertificateCatchingTrustManager tm = null;
        try {
            SSLContext context = SSLContext.getInstance("TLS");

            tm = new CertificateCatchingTrustManager();
            context.init(null, new TrustManager[]{tm}, null);
            SSLSocketFactory factory = context.getSocketFactory();

            SSLSocket socket = (SSLSocket) factory.createSocket(url.getHost(), url.getPort());
            socket.setSoTimeout(1000);

            socket.startHandshake();
            socket.close();
        } catch (Exception e) {
            Log.i(TAG, e.toString());
            e.printStackTrace();
        }

        if (tm == null || tm.chain.length < 1) {
            return null;
        }

        X509Certificate cert = tm.chain[0];

        byte[] certKey = new byte[0];
        try {
            certKey = cert.getEncoded();
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        }
        return Base64.encodeToString(certKey, Base64.DEFAULT);
    }

    private static class CertificateCatchingTrustManager implements X509TrustManager {
        private X509Certificate[] chain = new X509Certificate[]{};

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            this.chain = chain;
            throw new CertificateException();
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            throw new CertificateException();
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            throw new UnsupportedOperationException();
        }
    }
}
