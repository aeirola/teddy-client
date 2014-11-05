package fi.iki.aeirola.teddyclientlib.models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by aeirola on 15.10.2014.
 */
public class Line {
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm", Locale.US);

    public long windowId;
    public Date date;
    public String sender;
    public String message;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (date != null) {
            sb.append(DATE_FORMAT.format(date));
            sb.append(" ");
        }

        if (sender != null) {
            sb.append(sender);
            sb.append(": ");
        }

        if (message != null) {
            sb.append(stripEscapes(message));
        }

        return sb.toString();
    }

    private String stripEscapes(String input) {
        return input.replaceAll("\u0004./?", "");
    }
}
