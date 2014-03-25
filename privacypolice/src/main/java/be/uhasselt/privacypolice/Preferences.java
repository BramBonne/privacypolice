package be.uhasselt.privacypolice;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class Preferences {
    private SharedPreferences prefs;
    private final String ALLOWED_BSSID_PREFIX = "ABSSID//";

    public Preferences(Context ctx) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        Log.v("PrivacyPolice", "Current preferences are: " + prefs.getAll().toString());
    }

    public boolean getEnableOnlyAvailableNetworks() {
        return prefs.getBoolean("enableOnlyAvailableNetworks", true);
    }

    public boolean getOnlyConnectToKnownAccessPoints() {
        return prefs.getBoolean("onlyConnectToKnownAccessPoints", true);
    }

    public Set<String> getAllowedBSSIDs(String SSID) {
        return prefs.getStringSet(ALLOWED_BSSID_PREFIX + SSID, new HashSet<String>());
    }

    public Set<String> getBlockedBSSIDs() {
        return prefs.getStringSet("BlockedSSIDs", new HashSet<String>());
    }

    public void addAllowedBSSID(String SSID, String BSSID) {
        Set<String> currentlyInList = getAllowedBSSIDs(SSID);
        if (currentlyInList.contains(BSSID))
            // Already in the list
            return;

        Log.d("PrivacyPolice", "Adding BSSID: " + BSSID + " for " + SSID);
        currentlyInList.add(BSSID);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(ALLOWED_BSSID_PREFIX + SSID, currentlyInList);
        editor.commit();
    }

    public void addBlockedBSSID(String BSSID) {
        Set<String> currentlyInList = getBlockedBSSIDs();
        if (currentlyInList.contains(BSSID))
            // Already in the list
            return;

        Log.d("PrivacyPolice", "Adding blocked BSSID: " + BSSID);
        currentlyInList.add(BSSID);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("BlockedSSIDs", currentlyInList);
        editor.commit();
    }

}
