package fi.iki.aeirola.teddyclientlib.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by aeirola on 15.10.2014.
 */
public class Response implements Serializable {
    public String id;
    public String challenge;
    public Boolean login;

    public InfoResponse info;
    public WindowResponse window;
    public ItemResponse item;
    public LineResponse line;

    @JsonProperty("line added")
    public Map<Long, List<LineResponse.LineData>> lineAdded;
}
