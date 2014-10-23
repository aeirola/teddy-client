package fi.iki.aeirola.teddyclientlib;

import fi.iki.aeirola.teddyclientlib.models.request.HDataRequest;
import fi.iki.aeirola.teddyclientlib.models.request.InfoRequest;
import fi.iki.aeirola.teddyclientlib.models.request.InputRequest;
import fi.iki.aeirola.teddyclientlib.models.request.NickListRequest;

/**
 * Created by Axel on 21.10.2014.
 */
public class CommonRequest {
    public String challenge;
    public String login;
    public InputRequest.Input input;
    public InfoRequest.Info info;
    public HDataRequest.HData hdata;
    public NickListRequest.NickList nicklist;
}
