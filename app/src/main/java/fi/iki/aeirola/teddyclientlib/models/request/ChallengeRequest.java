package fi.iki.aeirola.teddyclientlib.models.request;

/**
 * Created by aeirola on 15.10.2014.
 */
public class ChallengeRequest extends BaseRequest {
    public String challenge;

    public ChallengeRequest(String challenge) {
        this.challenge = challenge;
    }
}
