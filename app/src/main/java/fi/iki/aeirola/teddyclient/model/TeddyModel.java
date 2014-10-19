package fi.iki.aeirola.teddyclient.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.aeirola.teddyclientlib.TeddyProtocolClient;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class TeddyModel {

    private static TeddyModel instance;

    public static TeddyModel getInstance() {
        if (instance == null) {
            instance = new TeddyModel();
        }

        return instance;
    }

    public final TeddyProtocolClient teddyProtocolClient;

    private TeddyModel() {
        URI uri = null;
        try {
            uri = new URI("ws://localhost:9001/teddy");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        teddyProtocolClient = new TeddyProtocolClient(uri, "s3cr3t");
        teddyProtocolClient.connect();
    }
}
