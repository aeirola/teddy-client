package fi.iki.aeirola.teddyclientlib;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import fi.iki.aeirola.teddyclientlib.models.Line;
import fi.iki.aeirola.teddyclientlib.models.Nick;
import fi.iki.aeirola.teddyclientlib.models.Window;

import static org.junit.Assert.assertTrue;

/**
 * Created by aeirola on 14.10.2014.
 */
public class TeddyProtocolTest {

    private URI uri;
    private CountDownLatch testLatch;

    private String receivedVersion;
    private List<Window> receivedWindowList;
    private List<Line> receivedLineList;
    private List<Nick> receivedNickList;

    @Before
    public void setUp() throws Exception {
        uri = new URI("ws://localhost:9001/teddy");
        testLatch = new CountDownLatch( 1 );
    }

    @Test
    public void connectToServerTest() throws URISyntaxException, InterruptedException {
        final TeddyProtocolClient teddyProtocol = new TeddyProtocolClient(this.uri, "s3cr3t");
        TeddyProtocolCallbackHandler callbackHandler = new TeddyProtocolCallbackHandler() {
            @Override
            public void onConnect() {
                testLatch.countDown();
            }
        };

        teddyProtocol.registerCallbackHandler(callbackHandler, "testHandler");

        teddyProtocol.connect();
        boolean success = testLatch.await(1, TimeUnit.SECONDS);
        teddyProtocol.closeBlocking();

        teddyProtocol.removeCallBackHandler("testHandler");
        assertTrue(success);
    }

    @Test
    public void loginToServerTest() throws URISyntaxException, InterruptedException {
        final TeddyProtocolClient teddyProtocol = new TeddyProtocolClient(this.uri, "s3cr3t");
        TeddyProtocolCallbackHandler callbackHandler = new TeddyProtocolCallbackHandler() {
            @Override
            public void onLogin() {
                testLatch.countDown();
            }
        };

        teddyProtocol.registerCallbackHandler(callbackHandler, "testHandler");

        teddyProtocol.connect();
        boolean success = testLatch.await(1, TimeUnit.SECONDS);
        teddyProtocol.closeBlocking();

        teddyProtocol.removeCallBackHandler("testHandler");
        assertTrue(success);
    }

    @Test
    public void versionTest() throws URISyntaxException, InterruptedException {
        final TeddyProtocolClient teddyProtocol = new TeddyProtocolClient(this.uri, "s3cr3t");
        TeddyProtocolCallbackHandler callbackHandler = new TeddyProtocolCallbackHandler() {
            @Override
            public void onVersion(String version) {
                receivedVersion = version;
                testLatch.countDown();
            }
        };

        teddyProtocol.registerCallbackHandler(callbackHandler, "testHandler");

        teddyProtocol.connect();
        teddyProtocol.requestVersion();
        boolean success = testLatch.await(1, TimeUnit.SECONDS);
        teddyProtocol.closeBlocking();

        teddyProtocol.removeCallBackHandler("testHandler");

        assertTrue(success);
        assertTrue(receivedVersion.contains("Irssi"));
    }


    @Test
    public void windowsTest() throws URISyntaxException, InterruptedException {
        final TeddyProtocolClient teddyProtocol = new TeddyProtocolClient(this.uri, "s3cr3t");
        TeddyProtocolCallbackHandler callbackHandler = new TeddyProtocolCallbackHandler() {
            @Override
            public void onLogin() {
                teddyProtocol.requestWindowList();
            }

            @Override
            public void onWindowList(List<Window> windowList) {
                receivedWindowList = windowList;
                testLatch.countDown();
            }
        };

        teddyProtocol.registerCallbackHandler(callbackHandler, "testHandler");

        teddyProtocol.connect();
        boolean success = testLatch.await(1, TimeUnit.SECONDS);
        teddyProtocol.closeBlocking();

        teddyProtocol.removeCallBackHandler("testHandler");

        assertTrue("Callback not received within timeout", success);
        assertTrue("First window item doesn't contain status", receivedWindowList.get(0).name.contains("status"));
    }


    @Test
    public void linesTest() throws URISyntaxException, InterruptedException {
        final TeddyProtocolClient teddyProtocol = new TeddyProtocolClient(this.uri, "s3cr3t");
        TeddyProtocolCallbackHandler callbackHandler = new TeddyProtocolCallbackHandler() {
            @Override
            public void onLogin() {
                teddyProtocol.requestWindowList();
            }

            @Override
            public void onWindowList(List<Window> windowList) {
                teddyProtocol.requestLineList(windowList.get(0));
            }

            @Override
            public void onLineList(List<Line> lineList) {
                receivedLineList = lineList;
                testLatch.countDown();
            }
        };

        teddyProtocol.registerCallbackHandler(callbackHandler, "testHandler");

        teddyProtocol.connect();
        boolean success = testLatch.await(1, TimeUnit.SECONDS);
        teddyProtocol.closeBlocking();

        teddyProtocol.removeCallBackHandler("testHandler");

        assertTrue("Callback not received within timeout", success);
        assertTrue("First line item doesn't contain mark", receivedLineList.get(0).message.contains("teddy"));
    }

    @Test
    public void nickListTest() throws URISyntaxException, InterruptedException {
        final TeddyProtocolClient teddyProtocol = new TeddyProtocolClient(this.uri, "s3cr3t");
        TeddyProtocolCallbackHandler callbackHandler = new TeddyProtocolCallbackHandler() {
            @Override
            public void onLogin() {
                teddyProtocol.requestWindowList();
            }

            @Override
            public void onWindowList(List<Window> windowList) {
                teddyProtocol.requestNickList(windowList.get(0));
            }

            @Override
            public void onNickList(List<Nick> nickList) {
                receivedNickList = nickList;
                testLatch.countDown();
            }
        };

        teddyProtocol.registerCallbackHandler(callbackHandler, "testHandler");

        teddyProtocol.connect();
        boolean success = testLatch.await(1, TimeUnit.SECONDS);
        teddyProtocol.closeBlocking();

        teddyProtocol.removeCallBackHandler("testHandler");

        assertTrue("Callback not received within timeout", success);
        assertTrue("First window item doesn't contain status", receivedNickList.get(0).name.contains("root"));
    }

}
