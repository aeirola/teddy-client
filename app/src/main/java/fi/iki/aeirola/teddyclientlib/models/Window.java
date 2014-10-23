package fi.iki.aeirola.teddyclientlib.models;

import java.io.Serializable;

/**
 * Created by aeirola on 15.10.2014.
 */
public class Window implements Serializable {
    public long id;
    public String name;
    public String fullName;

    @Override
    public String toString() {
        return this.name;
    }
}
