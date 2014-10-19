package fi.iki.aeirola.teddyclientlib.models.request;

/**
 * Created by aeirola on 15.10.2014.
 */
public class InputRequest {
    public Input input = new Input();

    public static class Input {
        public String path;
        public String data;
    }

    public InputRequest(String path, String data) {
        this.input.path = path;
        this.input.data = data;
    }

}
