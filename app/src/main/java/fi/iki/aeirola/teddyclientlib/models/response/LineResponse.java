package fi.iki.aeirola.teddyclientlib.models.response;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import fi.iki.aeirola.teddyclientlib.models.Line;

/**
 * Created by Axel on 18.11.2014.
 */
public class LineResponse implements Serializable {
    public Map<Long, List<LineData>> get;

    public static List<Line> toList(Map<Long, List<LineData>> lineMap) {
        if (lineMap == null) {
            return new ArrayList<>();
        }
        List<Line> lineList = new ArrayList<>();
        for (Map.Entry<Long, List<LineData>> entry : lineMap.entrySet()) {
            for (LineData lineData : entry.getValue()) {
                Line line = new Line();
                line.id = lineData.id;
                line.viewId = entry.getKey();
                line.date = new Date(lineData.time);
                line.message = lineData.text;
                lineList.add(line);
            }
        }
        return lineList;
    }

    public List<Line> toList() {
        return this.toList(this.get);
    }

    public static class LineData {
        public long id;
        public long time;
        public String text;
    }
}
