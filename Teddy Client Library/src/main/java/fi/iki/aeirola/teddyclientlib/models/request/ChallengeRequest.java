package fi.iki.aeirola.teddyclientlib.models.request;

import fi.iki.aeirola.teddyclientlib.models.BaseMessage;

/**
 * Created by aeirola on 15.10.2014.
 */
public class ChallengeRequest extends BaseMessage {
    public String challenge;

    public ChallengeRequest(String challenge) {
        this.challenge = challenge;
    }
}
