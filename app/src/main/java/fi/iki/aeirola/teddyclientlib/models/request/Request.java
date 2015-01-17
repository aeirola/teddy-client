package fi.iki.aeirola.teddyclientlib.models.request;

import java.io.Serializable;

/**
 * Created by Axel on 25.10.2014.
 */
public class Request implements Serializable {
    public Long id;

    public String challenge;
    public String login;

    public InfoRequest info;
    public WindowRequest window;
    public ItemRequest item;
    public LineRequest line;
    public InputRequest input;

    public boolean expectResponse() {
        return challenge != null ||
                login != null ||
                (info != null && info.expectResponse()) ||
                (window != null && window.expectResponse()) ||
                (item != null && item.expectResponse()) ||
                (line != null && line.expectResponse()) ||
                (input != null && input.expectResponse());
    }
}
