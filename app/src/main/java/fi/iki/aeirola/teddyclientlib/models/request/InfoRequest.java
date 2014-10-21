package fi.iki.aeirola.teddyclientlib.models.request;

/**
 * Created by aeirola on 15.10.2014.
 */
public class InfoRequest {

    public Info info = new Info();

    public InfoRequest(String version) {
        this.info.name = version;
    }

    public static class Info {
        public String name;
    }

}
