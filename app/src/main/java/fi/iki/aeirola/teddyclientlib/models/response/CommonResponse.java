package fi.iki.aeirola.teddyclientlib.models.response;

import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import fi.iki.aeirola.teddyclientlib.models.Line;
import fi.iki.aeirola.teddyclientlib.models.Nick;
import fi.iki.aeirola.teddyclientlib.models.Window;

/**
 * Created by aeirola on 15.10.2014.
 */
public class CommonResponse extends BaseResponse {

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public String challenge;
    public Boolean login;
    public InfoResponse info;
    public List<HDataResponse> hdata;
    public List<NickListResponse> nicklist;

    public HDataType getType() {
        if (this.hdata == null) {
            return null;
        }
        for (HDataResponse hdata : this.hdata) {
            if (hdata.highlight != null && hdata.buffer != null) {
                return HDataType.LINE;
            } else if (hdata.shortName != null) {
                return HDataType.WINDOW;
            }
        }

        return null;
    }

    public List<Window> toWindowList() {
        List<Window> windowList = new ArrayList<Window>(this.hdata.size());
        for (HDataResponse hdata : this.hdata) {
            Window window = new Window();
            window.id = hdata.pointers[1];
            window.name = hdata.shortName;
            window.fullName = hdata.fullName;
            windowList.add(window);
        }

        return windowList;
    }

    public List<Line> toLineList() {
        List<Line> lineList = new ArrayList<Line>(this.hdata.size());
        for (HDataResponse hdata : this.hdata) {
            Line line = new Line();
            line.windowId = hdata.buffer;
            try {
                line.date = DATE_FORMAT.parse(hdata.date);
            } catch (ParseException e) {
                Log.w("Response", "Failed to parse date: " + e);
            }
            if (!hdata.fromnick.equals("false")) {
                line.sender = hdata.fromnick;
            }
            line.message = hdata.message;
            lineList.add(line);
        }

        return lineList;
    }

    public List<Nick> toNickList() {
        List<Nick> nickList = new ArrayList<Nick>(this.nicklist.size());
        for (NickListResponse nickResponse : this.nicklist) {
            Nick nick = new Nick();
            nick.name = nickResponse.name;
            nickList.add(nick);
        }

        return nickList;
    }
}
