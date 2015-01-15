package fi.iki.aeirola.teddyclientlib.models;

import java.util.Date;

/**
 * Created by aeirola on 15.10.2014.
 */
public class Line {
    public long id;
    public Long prevId;
    public long viewId;
    public String message;
    public Date timestamp;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (message != null) {
            sb.append(stripEscapes(message));
        }

        return sb.toString();
    }

    private String stripEscapes(String input) {
        return input.replaceAll("\u0004./?", "");
    }
}
