package fi.iki.aeirola.teddyclientlib.models.request;

import java.io.Serializable;

/**
 * Created by aeirola on 15.10.2014.
 */
public class WindowRequest implements Serializable {
    public Get get = new Get();

    public static class Get {
        public int lv = 1;
        public String renumf = null;
    }
}
