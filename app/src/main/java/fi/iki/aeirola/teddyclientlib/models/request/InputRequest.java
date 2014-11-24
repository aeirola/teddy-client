package fi.iki.aeirola.teddyclientlib.models.request;

import java.io.Serializable;

/**
 * Created by aeirola on 15.10.2014.
 */
public class InputRequest implements Serializable {
    public Long window;
    public String data;
    public boolean active = false;

    public InputRequest() {

    }

    public InputRequest(Long windowId, String data) {
        this.window = windowId;
        this.data = data;
    }
}
