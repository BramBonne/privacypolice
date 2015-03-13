/* 
 * Copyright 2014, 2015 Bram Bonné
 *
 * This file is part of Wi-Fi PrivacyPolice.
 * 
 * Wi-Fi PrivacyPolice is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * Wi-Fi PrivacyPolice is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Wi-Fi PrivacyPolice.  If not, see <http://www.gnu.org/licenses/>.
*/

package be.uhasselt.privacypolice;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* Class used for storing and retreiving user preferences, including the list of trusted and
   untrusted access points
 */

public class PreferencesStorage {
    private SharedPreferences prefs;
    private WifiManager wifiManager;
    // String used to identify MAC addresses of allowed access points
    private final String ALLOWED_BSSID_PREFIX = "ABSSID//";

    public PreferencesStorage(Context ctx) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        this.wifiManager =  (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        try {
            Log.v("PrivacyPolice", "Current preferences are: " + prefs.getAll().toString());
        } catch (NullPointerException npe) {
            Log.d("PrivacyPolice", "No preferences found!");
        }
    }

    public boolean getEnableOnlyAvailableNetworks() {
        return prefs.getBoolean("enableOnlyAvailableNetworks", true);
    }

    public boolean getOnlyConnectToKnownAccessPoints() {
        return prefs.getBoolean("onlyConnectToKnownAccessPoints", false);
    }

    /**
     * Will be enabled if we decide to implement tracking
     * @return
     *
    public boolean getTrackingAllowed() {
        return prefs.getBoolean("trackingAllowed", false);
    } */

    /**
     * Get a list of trusted MAC addresses for a given SSID
       @param SSID the SSID of the network
     */
    public Set<String> getAllowedBSSIDs(String SSID) {
        return prefs.getStringSet(ALLOWED_BSSID_PREFIX + SSID, new HashSet<String>());
    }

    /**
     * Get a list of SSIDs for which we trust at least one BSSID
     */
    public Set<String> getKnownSSIDs() {
        Set<String> results = new HashSet<>();

        Map<String, ?> allPrefs = prefs.getAll();
        for (String key : allPrefs.keySet()) {
            if (key.startsWith(ALLOWED_BSSID_PREFIX)) {
                results.add(key.substring(ALLOWED_BSSID_PREFIX.length()));
            }
        }
        return results;
    }

    /**
     * Get a list of MAC addresses the user has chosen to block
     */
    public Set<String> getBlockedBSSIDs() {
        return prefs.getStringSet("BlockedSSIDs", new HashSet<String>());
    }

    /**
     * Adds all BSSIDs that are currently in range for the specified SSID (prevents nagging)
     * We are assuming the user does not know the BSSID of the AP it wants to trust, anyway, and
     * choose the more useable option.
     * @param SSID the SSID of the network that needs to be allowed at this location
     */
    public void addAllowedBSSIDsForLocation(String SSID) {
        List<ScanResult> scanResults = wifiManager.getScanResults();
        for (ScanResult result : scanResults) {
            if (SSID.equals(result.SSID))
                addAllowedBSSID(SSID, result.BSSID);
        }
    }

    /**
     * Add a specific MAC address as trusted for the given SSID
     * @param SSID the SSID of the network
     * @param BSSID the MAC address of the trusted access point
     */
    private void addAllowedBSSID(String SSID, String BSSID) {
        Set<String> currentlyInList = getAllowedBSSIDs(SSID);
        if (currentlyInList.contains(BSSID))
            // Already in the list
            return;

        // Create copy of list, because sharedPreferences only checks whether *reference* is the same
        // In order to add elements, we thus need a new object (otherwise nothing changes)
        Set<String> newList = new HashSet<>(currentlyInList);
        Log.i("PrivacyPolice", "Adding BSSID: " + BSSID + " for " + SSID);
        newList.add(BSSID);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(ALLOWED_BSSID_PREFIX + SSID, newList);
        editor.commit();
    }

    /**
     * Add an access point that we want to block connections to.
     * @param BSSID the MAC address of the untrusted access point
     */
    public void addBlockedBSSID(String BSSID) {
        Set<String> currentlyInList = getBlockedBSSIDs();
        if (currentlyInList.contains(BSSID))
            // Already in the list
            return;

        Log.i("PrivacyPolice", "Adding blocked BSSID: " + BSSID);
        // Create copy of list, because sharedPreferences only checks whether *reference* is the same
        // In order to add elements, we thus need a new object (otherwise nothing changes)
        Set<String> newList = new HashSet<>(currentlyInList);
        newList.add(BSSID);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("BlockedSSIDs", newList);
        editor.commit();
    }

    /**
     * Erase all trusted and untrusted hotspots.
     */
    public void clearBSSIDLists() {
        Log.d("PrivacyPolice", "Removing all trusted/untrusted hotspots");
        SharedPreferences.Editor editor = prefs.edit();

        // Erase all allowed SSIDs, by emptying their MAC address lists.
        for (String key: prefs.getAll().keySet()) {
            if (key.startsWith(ALLOWED_BSSID_PREFIX))
                editor.putStringSet(key, new HashSet<String>());
        }

        // Erase blocked SSIDs
        editor.putStringSet("BlockedSSIDs", new HashSet<String>());

        editor.commit();
    }
}
