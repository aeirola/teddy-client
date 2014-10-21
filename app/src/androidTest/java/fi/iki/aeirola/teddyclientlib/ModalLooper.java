package fi.iki.aeirola.teddyclientlib;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Created by Axel on 21.10.2014.
 */
public class ModalLooper {

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    throw new ReturnException();
                case 1:
                    throw new TimeoutException();
            }
        }
    };

    public boolean loop(int timeout) {
        try {
            if (timeout > 0) {
                handler.sendEmptyMessageDelayed(1, timeout);
            }
            Looper.loop();
        } catch (ReturnException e) {
            // normal exit
        } catch (TimeoutException e) {
            return false;
        }
        return true;
    }

    public boolean loop() {
        return loop(0);
    }

    public void stop() {
        handler.sendEmptyMessage(0);
    }

    public class ReturnException extends RuntimeException {
    }

    public class TimeoutException extends RuntimeException {
    }

}
