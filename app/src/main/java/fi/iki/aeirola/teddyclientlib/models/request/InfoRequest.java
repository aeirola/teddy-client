package fi.iki.aeirola.teddyclientlib.models.request;

import java.io.Serializable;

/**
 * Created by aeirola on 15.10.2014.
 */
public class InfoRequest implements Serializable {
    public String name;

    public InfoRequest() {

    }

    public InfoRequest(String name) {
        this.name = name;
    }

    public boolean expectResponse() {
        return name != null;
    }
}
