package fi.iki.aeirola.teddyclientlib.models.request;

import java.io.Serializable;

/**
 * Created by Axel on 24.11.2014.
 */
public class ItemRequest implements Serializable {
    public Get get = new Get();

    public static class Get {
        public int lv = 1;
    }
}
