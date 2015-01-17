package fi.iki.aeirola.teddyclientlib.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by Axel on 17.1.2015.
 */
public class TimeoutHandler {

    private final long timeout;
    private final TimeoutCallbackHandler timeoutCallback;
    private final TimeoutRunner timeoutRunner = new TimeoutRunner();
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> timeoutFuture;

    public TimeoutHandler(long timeout, TimeoutCallbackHandler onTimeout) {
        this.timeout = timeout;
        this.timeoutCallback = onTimeout;
    }

    public void set() {
        if (timeoutFuture == null) {
            timeoutFuture = worker.schedule(timeoutRunner, timeout, TimeUnit.MILLISECONDS);
        }
    }

    public void cancel() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
            timeoutFuture = null;
        }
    }

    public void reset() {
        cancel();
        set();
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
