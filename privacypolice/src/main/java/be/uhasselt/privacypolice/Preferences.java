package be.uhasselt.privacypolice;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {
    private SharedPreferences prefs;

    public Preferences(Context ctx) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public boolean getEnableOnlyAvailableNetworks() {
        return prefs.getBoolean("enableOnlyAvailableNetworks", true);
    }
}
