package fi.iki.aeirola.teddyclientlib.models.request;

/**
 * Created by aeirola on 15.10.2014.
 */
public class InputRequest extends BaseRequest {
    public Input input = new Input();

    public InputRequest(String buffer, String data) {
        this.input.buffer = buffer;
        this.input.data = data;
    }

    public static class Input {
        public String buffer;
        public String data;
    }

}
