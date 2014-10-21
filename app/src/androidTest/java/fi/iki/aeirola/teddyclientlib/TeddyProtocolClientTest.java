package fi.iki.aeirola.teddyclientlib;

import junit.framework.TestCase;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import fi.iki.aeirola.teddyclientlib.models.Line;
import fi.iki.aeirola.teddyclientlib.models.Nick;
import fi.iki.aeirola.teddyclientlib.models.Window;

/**
 * Created by aeirola on 14.10.2014.
 */
public class TeddyProtocolClientTest extends TestCase {
    private static final int TIMEOUT = 5000;

    private String uri;
    private TestServer server;

    private CountDownLatch testLatch;
    private ModalLooper modal;
    private TeddyProtocolClient teddyProtocol;

    private String receivedVersion;
    private List<Window> receivedWindowList;
    private List<Line> receivedLineList;
    private List<Nick> receivedNickList;

    @Override
    public void setUp() throws Exception {
        this.server = new TestServer(new InetSocketAddress("localhost", 8080));
        this.server.start();
        // Wait for server to start
        Thread.sleep(100);

        this.uri = "ws://localhost:8080/";
        this.testLatch = new CountDownLatch(1);
        modal = new ModalLooper();
    }

    @Override
    protected void tearDown() throws Exception {
        this.server.stop(TIMEOUT);
    }

    protected void runTest(TeddyProtocolCallbackHandler callbackHandler) throws InterruptedException {
        teddyProtocol = new TeddyProtocolClient(this.uri, "s3cr3t");
        teddyProtocol.registerCallbackHandler(callbackHandler, "testHandler");

        teddyProtocol.connect();
        modal.loop(TIMEOUT);
        boolean success = testLatch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        teddyProtocol.disconnect();

        teddyProtocol.removeCallBackHandler("testHandler");
        assertTrue("Callback not received within timeout", success);
    }

    protected void endTest() {
        testLatch.countDown();
        modal.stop();
    }

    public void testConnectToServer() throws InterruptedException {
        this.runTest(new TeddyProtocolCallbackHandler() {
            @Override
            public void onConnect() {
                TeddyProtocolClientTest.this.endTest();
            }
        });
    }

    public void testLoginToServer() throws URISyntaxException, InterruptedException {
        this.runTest(new TeddyProtocolCallbackHandler() {
            @Override
            public void onLogin() {
                TeddyProtocolClientTest.this.endTest();
            }
        });
    }

    public void testVersion() throws URISyntaxException, InterruptedException {
        this.runTest(new TeddyProtocolCallbackHandler() {
            @Override
            public void onLogin() {
                teddyProtocol.requestVersion();
            }

            @Override
            public void onVersion(String version) {
                receivedVersion = version;
                TeddyProtocolClientTest.this.endTest();
            }
        });
        assertTrue(receivedVersion.equals("test-server-1.0"));
    }

    public void testWindows() throws URISyntaxException, InterruptedException {
        this.runTest(new TeddyProtocolCallbackHandler() {
            @Override
            public void onLogin() {
                teddyProtocol.requestWindowList();
            }

            @Override
            public void onWindowList(List<Window> windowList) {
                receivedWindowList = windowList;
                TeddyProtocolClientTest.this.endTest();
            }
        });
        assertTrue("First window item doesn't contain status", receivedWindowList.get(0).name.contains("status"));
    }

    public void testLines() throws URISyntaxException, InterruptedException {
        this.runTest(new TeddyProtocolCallbackHandler() {
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
                TeddyProtocolClientTest.this.endTest();
            }
        });

        assertTrue("First line item doesn't contain mark", receivedLineList.get(0).message.contains("hello"));
    }

    public void testNickList() throws URISyntaxException, InterruptedException {
        this.runTest(new TeddyProtocolCallbackHandler() {
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
                TeddyProtocolClientTest.this.endTest();
            }
        });

        assertTrue("First window item doesn't contain status", receivedNickList.get(0).name.contains("test_user"));
    }

}
