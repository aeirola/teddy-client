package fi.iki.aeirola.teddyclientlib.models;

import java.io.Serializable;

/**
 * Created by aeirola on 15.10.2014.
 */
public class Window implements Serializable {
    public long id;
    public long viewId;
    public String name;
    public String fullName;
    public Activity activity;

    @Override
    public String toString() {
        return this.name;
    }

    public enum Activity {
        INACTIVE,
        PASSIVE,
        ACTIVE,
        HILIGHT
    }

}
