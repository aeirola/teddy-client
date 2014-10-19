package fi.iki.aeirola.teddyclientlib.models.response;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fi.iki.aeirola.teddyclientlib.models.BaseMessage;
import fi.iki.aeirola.teddyclientlib.models.Line;
import fi.iki.aeirola.teddyclientlib.models.Nick;
import fi.iki.aeirola.teddyclientlib.models.Window;

/**
 * Created by aeirola on 15.10.2014.
 */
public class CommonResponse extends BaseMessage {

    public String challenge;
    public Boolean login;
    public InfoResponse info;
    public List<HDataResponse> hdata;
    public List<NickListResponse> nicklist;

    public HDataType getType() {
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
            line.date = new Date();
            line.sender = hdata.fromNick;
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
