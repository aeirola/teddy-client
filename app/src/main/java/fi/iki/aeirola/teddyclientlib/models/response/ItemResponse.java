package fi.iki.aeirola.teddyclientlib.models.response;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Axel on 24.11.2014.
 */
public class ItemResponse implements Serializable {
    public List<ItemData> get;

    public static enum ItemType {
        CHANNEL,
        QUERY
    }

    public static class ItemData {
        public long id;
        public long window;
        public ItemType type;
        public String visibleName;
        public String topic;
    }
}
