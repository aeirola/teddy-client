package fi.iki.aeirola.teddyclientlib.models.request;

import fi.iki.aeirola.teddyclientlib.models.BaseMessage;

/**
 * Created by aeirola on 15.10.2014.
 */
public class LoginRequest extends BaseMessage {
    public String login;

    public LoginRequest(String login) {
        this.login = login;
    }
}
