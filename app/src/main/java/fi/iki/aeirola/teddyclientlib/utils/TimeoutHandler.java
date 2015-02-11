package fi.iki.aeirola.teddyclientlib.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by Axel on 17.1.2015.
 */
public class TimeoutHandler {

    private static ScheduledExecutorService worker;

    private final long timeout;
    private final TimeoutCallbackHandler timeoutCallback;
    private final TimeoutRunner timeoutRunner = new TimeoutRunner();
    private boolean enabled = true;
    private ScheduledFuture<?> timeoutFuture;

    public TimeoutHandler(long timeout, TimeoutCallbackHandler onTimeout) {
        this.timeout = timeout;
        this.timeoutCallback = onTimeout;
    }

    private static ScheduledExecutorService getWorker() {
        if (worker == null) {
            worker = Executors.newSingleThreadScheduledExecutor();
        }
        return worker;
    }

    public void set() {
        if (enabled && timeoutFuture == null) {
            timeoutFuture = getWorker().schedule(timeoutRunner, timeout, TimeUnit.MILLISECONDS);
        }
    }

    public void cancel() {
        if (enabled && timeoutFuture != null) {
            timeoutFuture.cancel(false);
            timeoutFuture = null;
        }
    }

    public void reset() {
        cancel();
        set();
    }

    public void disable() {
        this.cancel();
        this.enabled = false;
    }

    public void enable() {
        this.enabled = true;
        this.set();
    }

    public interface TimeoutCallbackHandler {
        public void onTimeout();
    }

    private class TimeoutRunner implements Runnable {
        @Override
        public void run() {
            timeoutCallback.onTimeout();
        }
    }
}
