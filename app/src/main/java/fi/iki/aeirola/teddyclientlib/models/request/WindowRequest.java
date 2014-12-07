package fi.iki.aeirola.teddyclientlib.models.request;

import java.io.Serializable;
import java.util.List;

/**
 * Created by aeirola on 15.10.2014.
 */
public class WindowRequest implements Serializable {
    public Get get;
    public List<Long> dehilight;

    public static class Get {
        public int lv = 1;
        public String renumf = null;
    }
}
