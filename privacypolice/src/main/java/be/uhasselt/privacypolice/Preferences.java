package be.uhasselt.privacypolice;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Set;

public class Preferences {
    private SharedPreferences prefs;

    public Preferences(Context ctx) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public boolean getEnableOnlyAvailableNetworks() {
        return prefs.getBoolean("enableOnlyAvailableNetworks", true);
    }

    public boolean getOnlyConnectToKnownAccessPoints() {
        return prefs.getBoolean("onlyConnectToKnownAccessPoints", false);
    }

    public HashMap<String, Set> getAllowedBSSIDs() {
        // TODO: IMPLEMENT ME
        return new HashMap<String, Set>();
    }
}
