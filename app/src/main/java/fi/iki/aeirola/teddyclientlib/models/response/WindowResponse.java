package fi.iki.aeirola.teddyclientlib.models.response;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import fi.iki.aeirola.teddyclientlib.models.Window;

/**
 * Created by Axel on 18.11.2014.
 */
public class WindowResponse implements Serializable {
    public List<WindowData> get;

    public List<Window> toList(ItemResponse item) {
        if (this.get == null) {
            return new ArrayList<>();
        }

        List<Window> windowList = new ArrayList<>(this.get.size());
        for (WindowData data : this.get) {
            List<ItemResponse.ItemData> itemDatas = new ArrayList<>();
            if (item != null) {
                for (ItemResponse.ItemData itemData : item.get) {
                    if (itemData.window == data.id) {
                        itemDatas.add(itemData);
                    }
                }
            }

            Window window = new Window();
            window.id = data.id;
            window.viewId = data.view;
            if (!data.name.isEmpty()) {
                window.name = data.refnum + ": " + data.name;
            } else if (!itemDatas.isEmpty()) {
                window.name = data.refnum + ": " + itemDatas.get(0).visibleName;
            } else {
                window.name = String.valueOf(data.refnum);
            }
            window.activity = Window.Activity.values()[data.dataLevel];
            windowList.add(window);
        }

        return windowList;
    }

    public static class WindowData {
        public long id;
        public long view;
        public int refnum;
        public String name;
        public int dataLevel;
    }
}
