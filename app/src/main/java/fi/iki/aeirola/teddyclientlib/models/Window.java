package fi.iki.aeirola.teddyclientlib.models;

/**
 * Created by aeirola on 15.10.2014.
 */
public class Window {
    public long id;
    public String name;
    public String fullName;

    @Override
    public String toString() {
        return this.name;
    }
}
