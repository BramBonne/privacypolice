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
    private final String BLOCKED_BSSID_PREFIX = "BBSSID//";

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

    public boolean getLocationNoticeEnabled() {
        return prefs.getBoolean("showLocationNotice", true);
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
        return getBSSIDs(SSID, true);
    }

    /**
     * Get a list of blocked MAC addresses for a given SSID
     @param SSID the SSID of the network
     */
    public Set<String> getBlockedBSSIDs(String SSID) {
        return getBSSIDs(SSID, false);
    }

    /**
     * Helper function for getAllowedBSSIDs and getBlockedSSIDs
     @param SSID the SSID of the network
     @param allowed when true, return the allowed BSSIDs, when false, return blocked BSSIDs
     */
    private Set<String> getBSSIDs(String SSID, boolean allowed) {
        String prefix;
        if (allowed)
            prefix = ALLOWED_BSSID_PREFIX;
        else
            prefix = BLOCKED_BSSID_PREFIX;
        // Return a copy so the receiver cannot edit the list
        return new HashSet<>(prefs.getStringSet(prefix + SSID, new HashSet<String>()));
    }

    /**
     * Get a list of SSIDs for which we remembered at least one BSSID (either allowed or blocked)
     */
    public Set<String> getNonemptySSIDs() {
        Set<String> results = new HashSet<>();

        Map<String, ?> allPrefs = prefs.getAll();
        for (String key : allPrefs.keySet()) {
            if (key.startsWith(ALLOWED_BSSID_PREFIX) && prefs.getStringSet(key, new HashSet<String>()).size() > 0) {
                results.add(key.substring(ALLOWED_BSSID_PREFIX.length()));
            } else if (key.startsWith(BLOCKED_BSSID_PREFIX) && prefs.getStringSet(key, new HashSet<String>()).size() > 0) {
                results.add(key.substring(BLOCKED_BSSID_PREFIX.length()));
            }
        }
        return results;
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

    public void addAllowedBSSID(String SSID, String BSSID) {
        Log.i("PrivacyPolice", "Adding allowed BSSID " + BSSID + " for network " + SSID);
        editBSSID(SSID, BSSID, true, true);
    }

    public void addBlockedBSSID(String SSID, String BSSID) {
        Log.i("PrivacyPolice", "Adding blocked BSSID " + BSSID + " for network " + SSID);
        editBSSID(SSID, BSSID, false, true);
    }

    /**
     * Remove a specific MAC address as trusted for the given SSID
     * @param SSID the SSID of the network
     * @param BSSID the MAC address of the trusted access point
     */
    public void removeAllowedBSSID(String SSID, String BSSID) {
        Log.i("PrivacyPolice", "Removing allowed BSSID " + BSSID + " for network " + SSID);
        editBSSID(SSID, BSSID, true, false);
    }

    public void removeBlockedBSSID(String SSID, String BSSID) {
        Log.i("PrivacyPolice", "Removing blocked BSSID " + BSSID + " for network " + SSID);
        editBSSID(SSID, BSSID, false, false);
    }

    /**
     * Helper function for addAllowedBSSID, addBlockedSSID, removedAllowedBSSID and removeBlockedBSSID
     * @param SSID the SSID of the network
     * @param BSSID the MAC address of the trusted access point
     * @param allowed when true, add to allowed BSSIDs, when false, add to blocked BSSIDs
     * @param add when true, add the network to the list, when false, remove the network
     */
    private void editBSSID(String SSID, String BSSID, boolean allowed, boolean add) {
        // Get the correct prefix (allowed or blocked hotspots)
        String prefix;
        if (allowed)
            prefix = ALLOWED_BSSID_PREFIX;
        else
            prefix = BLOCKED_BSSID_PREFIX;

        // Get the current list of known networks
        Set<String> currentlyInList = prefs.getStringSet(prefix + SSID, new HashSet<String>());
        // Create copy of list, because sharedPreferences only checks whether *reference* is the same
        // In order to add elements, we thus need a new object (otherwise nothing changes)
        Set<String> newList = new HashSet<>(currentlyInList);
        if (add)
            newList.add(BSSID);
        else
            newList.remove(BSSID);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(prefix + SSID, newList);
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
            if (key.startsWith(ALLOWED_BSSID_PREFIX) || key.startsWith(BLOCKED_BSSID_PREFIX))
                editor.putStringSet(key, new HashSet<String>());
        }

        editor.commit();
    }

    /**
     * Erase all known hotspots for a specific SSID.
     */
    public void clearBSSIDsForNetwork(String SSID) {
        Log.d("PrivacyPolice", "Removing all known hotspots for network " + SSID);
        SharedPreferences.Editor editor = prefs.edit();

        // Erase all trusted network for this SSID, by emptying its MAC address list.
        editor.putStringSet(ALLOWED_BSSID_PREFIX + SSID, new HashSet<String>());
        // Erase all blocked network for this SSID
        editor.putStringSet(BLOCKED_BSSID_PREFIX + SSID, new HashSet<String>());

        editor.commit();
    }
}
