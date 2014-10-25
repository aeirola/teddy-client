package fi.iki.aeirola.teddyclientlib.models.request;

/**
 * Created by aeirola on 15.10.2014.
 */
public class NickListRequest extends BaseRequest {
    public NickList nicklist = new NickList();

    public NickListRequest(String buffer) {
        this.nicklist.buffer = buffer;
    }

    public static class NickList {
        public String buffer;
    }
}
