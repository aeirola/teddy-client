package fi.iki.aeirola.teddyclientlib;

import java.util.List;

import fi.iki.aeirola.teddyclientlib.models.Line;
import fi.iki.aeirola.teddyclientlib.models.Nick;
import fi.iki.aeirola.teddyclientlib.models.Window;

public abstract class TeddyProtocolCallbackHandler {

    public void onConnect() {
    }

    public void onLogin() {
    }

    public void onVersion(String version) {
    }

    public void onClose() {
    }

    public void onWindowList(List<Window> windowList) {
    }

    public void onLineList(List<Line> lineList) {
    }

    public void onNickList(List<Nick> nickList) {
    }
}
