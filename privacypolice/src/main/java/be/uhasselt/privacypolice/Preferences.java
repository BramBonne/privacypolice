package be.uhasselt.privacypolice;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {
    private SharedPreferences prefs;

    public Preferences(Context ctx) {
        this.prefs = ctx.getSharedPreferences("PrivacyPolice", 0);
    }

    public boolean getEnableOnlyAvailableNetworks() {
        return prefs.getBoolean("enableOnlyAvailableNetworks", true);
    }

    public void setEnableOnlyAvailableNetworks(boolean value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("enableOnlyAvailableNetworks", value);
        editor.commit();
    }
}
