package fi.iki.aeirola.teddyclientlib.models.request;

/**
 * Created by aeirola on 15.10.2014.
 */
public class LoginRequest extends BaseRequest {
    public String login;

    public LoginRequest(String login) {
        this.login = login;
    }
}
