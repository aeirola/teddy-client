package fi.iki.aeirola.teddyclientlib.models.response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import fi.iki.aeirola.teddyclientlib.models.Line;
import fi.iki.aeirola.teddyclientlib.models.Window;

/**
 * Created by aeirola on 15.10.2014.
 */
public class HDataResponse {
    // Common
    public long[] pointers;


    // Windows
    public Integer number;
    public String title;
    public String shortName;
    public String fullName;
    public LocalVariables localVariables;

    public static class LocalVariables {
        public String type;
        public String visibleName;
        public String name;
    }


    // Lines
    public Integer displayed;
    public String date;
    public Long buffer;
    public String fromNick;
    public String strtime;
    public String message;
    public List<String> tagsArray;
    public Integer highlight;
    public String prefix;
}
