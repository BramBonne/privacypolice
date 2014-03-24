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
    private final String BLOCKED_BSSID_PREFIX = "BBSSID//";

    public Preferences(Context ctx) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public boolean getEnableOnlyAvailableNetworks() {
        return prefs.getBoolean("enableOnlyAvailableNetworks", true);
    }

    public boolean getOnlyConnectToKnownAccessPoints() {
        return prefs.getBoolean("onlyConnectToKnownAccessPoints", false);
    }

    public Set<String> getAllowedBSSIDs(String SSID) {
        return prefs.getStringSet(ALLOWED_BSSID_PREFIX + SSID, new HashSet<String>());
    }

    public Set<String> getBlockedBSSIDs(String SSID) {
        return prefs.getStringSet(BLOCKED_BSSID_PREFIX + SSID, new HashSet<String>());
    }

    public void addAllowedBSSID(String SSID, String BSSID) {
        addBSSIDToList(SSID, BSSID, true);
    }

    public void addBlockedBSSID(String SSID, String BSSID) {
        addBSSIDToList(SSID, BSSID, false);
    }

    private void addBSSIDToList(String SSID, String BSSID, boolean allowed) {
        String listPrefix;
        if (allowed)
            listPrefix = ALLOWED_BSSID_PREFIX;
        else
            listPrefix = BLOCKED_BSSID_PREFIX;

        Set<String> currentlyInList = prefs.getStringSet(listPrefix + SSID, new HashSet<String>());
        if (currentlyInList.contains(BSSID))
            // Already in the list
            return;

        Log.d("PrivacyPolice", "Adding BSSID: " + BSSID + " for " + SSID + " (" + allowed + ")");
        currentlyInList.add(BSSID);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(ALLOWED_BSSID_PREFIX + SSID, currentlyInList);
        editor.commit();
    }
}
