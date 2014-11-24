package fi.iki.aeirola.teddyclientlib.models.request;

import java.io.Serializable;

/**
 * Created by Axel on 25.10.2014.
 */
public class Request implements Serializable {
    public String id;

    public String challenge;
    public String login;

    public InfoRequest info;
    public WindowRequest window;
    public ItemRequest item;
    public LineRequest line;
    public InputRequest input;
}
