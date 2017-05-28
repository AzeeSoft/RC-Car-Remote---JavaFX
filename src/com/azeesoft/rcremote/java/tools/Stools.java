package com.azeesoft.rcremote.java.tools;

import java.util.prefs.Preferences;

/**
 * Created by azizt on 5/27/2017.
 */
public class Stools {
    public static Preferences getPreferences() {
        return Preferences.userRoot().node("rcremote");
    }

    public static void log(String msg) {
        System.out.println("RCRemote: " + msg);
    }

    public static void log(String tag, String msg) {
        System.out.println(tag + ": " + msg);
    }

}
