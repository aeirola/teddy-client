package fi.iki.aeirola.teddyclientlib.models.request;

/**
 * Created by aeirola on 15.10.2014.
 */
public class HDataRequest {
    public HData hdata = new HData();

    public HDataRequest(String path) {
        this.hdata.path = path;
    }

    public static class HData {
        public String path;
    }
}