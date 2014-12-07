package fi.iki.aeirola.teddyclientlib.models;

/**
 * Created by aeirola on 15.10.2014.
 */
public class Line {
    public long id;
    public long viewId;
    public String message;

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
