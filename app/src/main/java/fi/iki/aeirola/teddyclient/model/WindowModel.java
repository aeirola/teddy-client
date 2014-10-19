package fi.iki.aeirola.teddyclient.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aeirola on 13.10.2014.
 */
public class WindowModel {

    private static List<String> LINES;
    {
        LINES = new ArrayList<String>();
        LINES.add("20:55 -!- Irssi: Looking up eu.irc6.net");
        LINES.add("20:55 -!- Irssi: Connecting to eu.irc6.net [2001:16d8:27::] port 6667");
        LINES.add("20:55 -!- Irssi: Unable to connect server eu.irc6.net port 6667 [No route to host]");
        LINES.add("20:55 -!- Irssi: Removed reconnection to server open.ircnet.net port 6667");
        LINES.add("20:55 -!- Irssi: Looking up open.ircnet.net");
        LINES.add("20:55 -!- Irssi: Reconnecting to open.ircnet.net [91.217.189.58] port 6667 - use /RMRECONNS to abort");
        LINES.add("20:55 -!- Irssi: Connection to open.ircnet.net established");
        LINES.add("20:55 -!- Please wait while we process your connection.");
        LINES.add("20:55 -!- Welcome to the Internet Relay Network aeirola!~aeirola@84-253-222-127.bb.dnainternet.fi");
        LINES.add("20:55 -!- Your host is irc.portlane.se, running version 2.11.2p3");
        LINES.add("20:55 -!- This server was created Tue Jul 29 2014 at 20:26:28 CEST");
        LINES.add("20:55 -!- irc.portlane.se 2.11.2p3 aoOirw abeiIklmnoOpqrRstv");
        LINES.add("20:55 -!- RFC2812 PREFIX=(ov)@+ CHANTYPES=#&!+ MODES=3 CHANLIMIT=#&!+:42 NICKLEN=15 TOPICLEN=255 KICKLEN=255 MAXLIST=beIR:64 CHANNELLEN=50 IDCHAN=!:5 CHANMODES=beIR,k,l,imnpstaqr are supported by this server");
        LINES.add("20:55 -!- PENALTY FNC EXCEPTS=e INVEX=I CASEMAPPING=ascii NETWORK=IRCnet are supported by this server");
        LINES.add("20:55 -!- 0PNSACT8T your unique ID");
        LINES.add("20:55 -!- There are 49894 users and 4 services on 32 servers");
        LINES.add("20:55 -!- 102 operators online");
        LINES.add("20:55 -!- 8 unknown connections");
        LINES.add("20:55 -!- 27394 channels formed");
        LINES.add("20:55 -!- I have 1179 users, 0 services and 1 servers");
        LINES.add("20:55 -!- 1179 1205 Current local users 1179, max 1205");
        LINES.add("20:55 -!- 49894 52778 Current global users 49894, max 52778");
    }

    public String name;

    public WindowModel(String name) {
        this.name = name;
    }

    public List<String> getLines() {
        return this.LINES;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
