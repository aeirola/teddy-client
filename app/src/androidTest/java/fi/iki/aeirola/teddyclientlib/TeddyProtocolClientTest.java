package fi.iki.aeirola.teddyclientlib;

import junit.framework.TestCase;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import fi.iki.aeirola.teddyclientlib.models.Line;
import fi.iki.aeirola.teddyclientlib.models.Window;

/**
 * Created by aeirola on 14.10.2014.
 */
public class TeddyProtocolClientTest extends TestCase {
    private static final int TIMEOUT = 5000;

    private String uri;
    private TestServer server;

    private CountDownLatch testLatch;
    private TeddyClient teddyProtocol;

    private String receivedVersion;
    private List<Window> receivedWindowList;
    private List<Line> receivedLineList;

    @Override
    public void setUp() throws Exception {
        this.server = new TestServer(new InetSocketAddress("localhost", 8080));
        this.server.start();
        // Wait for server to start
        Thread.sleep(100);

        this.uri = "ws://localhost:8080";
        this.testLatch = new CountDownLatch(1);
    }

    @Override
    protected void tearDown() throws Exception {
        this.server.stop(TIMEOUT);
    }

    protected void runTest(TeddyCallbackHandler callbackHandler) throws InterruptedException {
        teddyProtocol = new TeddyClient(this.uri, "s3cr3t");
        teddyProtocol.registerCallbackHandler(callbackHandler, "testHandler");

        teddyProtocol.connect();
        boolean success = testLatch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        teddyProtocol.disconnect();

        teddyProtocol.removeCallBackHandler("testHandler");
        assertTrue("Callback not received within timeout", success);
    }

    protected void endTest() {
        testLatch.countDown();
    }

    public void testConnectToServer() throws InterruptedException {
        this.runTest(new TeddyCallbackHandler() {
            @Override
            public void onConnect() {
                TeddyProtocolClientTest.this.endTest();
            }
        });
    }

    public void testLoginToServer() throws URISyntaxException, InterruptedException {
        this.runTest(new TeddyCallbackHandler() {
            @Override
            public void onLogin() {
                TeddyProtocolClientTest.this.endTest();
            }
        });
    }

    public void testVersion() throws URISyntaxException, InterruptedException {
        this.runTest(new TeddyCallbackHandler() {
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
        this.runTest(new TeddyCallbackHandler() {
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
        this.runTest(new TeddyCallbackHandler() {
            @Override
            public void onLogin() {
                teddyProtocol.requestWindowList();
            }

            @Override
            public void onWindowList(List<Window> windowList) {
                teddyProtocol.requestLineList(windowList.get(0).id);
            }

            @Override
            public void onLineList(List<Line> lineList) {
                receivedLineList = lineList;
                TeddyProtocolClientTest.this.endTest();
            }
        });

        assertTrue("First line item doesn't contain mark", receivedLineList.get(0).message.contains("hello"));
    }

    public void testInput() throws URISyntaxException, InterruptedException {
        final String input = "hello to you too!";
        this.runTest(new TeddyCallbackHandler() {
            @Override
            public void onLogin() {
                teddyProtocol.requestWindowList();
            }

            @Override
            public void onWindowList(List<Window> windowList) {
                Window window = windowList.get(0);
                teddyProtocol.subscribeLines(window.viewId);
                teddyProtocol.sendInput(window.id, input);
            }

            @Override
            public void onLineList(List<Line> lineList) {
                receivedLineList = lineList;
                endTest();
            }
        });

        assertTrue("Lines doesn't contain input", receivedLineList.get(0).message.endsWith(input));
    }

}
