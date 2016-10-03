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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

import java.util.List;
import java.util.Set;

/**
 * This class contains the actual logic for deciding whether a network connection is allowed.
 * It receives the broadcast intents for new scan results, in order to decide whether a network is
 * available. It then checks whether we trust the AP's MAC address, based on the user's configuration.
 */

public class ScanResultsChecker extends BroadcastReceiver {

    public enum AccessPointSafety {
        TRUSTED, UNTRUSTED, UNKNOWN
    }

    // The last time we checked all networks.
    private static long lastCheck = 0;
    private static PreferencesStorage prefs = null;
    private static WifiManager wifiManager = null;
    private static ConnectivityManager connectivityManager = null;
    private static NotificationHandler notificationHandler = null;
    private static Context context = null;

    /**
     * Default constructor allowing to use this class as a receiver.
     * DO NOT USE THIS CONSTRUCTOR WHEN INSTANTIATING THIS CLASS MANUALLY. Pass the context as the
     * single parameter to this constructor instead.
     */
    public ScanResultsChecker() {
        super();
    }

    /**
     * Non-default constructor which allows other classes to instantiate this class with a given context
     * @param ctx Context of the caller
     */
    public ScanResultsChecker(Context ctx) {
        init(ctx);
    }

    /**
     * Initialize static variables, depending on the current context
     * @param ctx The current context
     */
    public void init(Context ctx) {
        wifiManager =  (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        prefs = new PreferencesStorage(ctx);
        notificationHandler = new NotificationHandler(ctx);
        context = ctx;
    }

    /**
     * Called for the following intents:
     *  - SCAN_RESULTS available
     *  - BOOT_COMPLETED
     */
    public void onReceive(Context ctx, Intent intent) {
        if (wifiManager == null) // TODO: make this class a singleton
            init(ctx);
        // Make sure the wakelockHandler keeps running (to prevent Android 6.0 and up from completely suspending our operations)
        WakelockHandler.getInstance(ctx).ensureAwake();

        // WiFi scan performed
        // Older devices might try to scan constantly. Allow them some rest by checking max. once every 0.5 seconds
        if (System.currentTimeMillis() - lastCheck < 500)
            return;
        lastCheck = System.currentTimeMillis();

        try {
            List<ScanResult> scanResults = wifiManager.getScanResults();
            Log.d("PrivacyPolice", "Wi-Fi scan performed, results are: " + scanResults.toString());
            checkResults(scanResults);
        } catch (NullPointerException npe) {
            Log.e("PrivacyPolice", "Null pointer exception when handling networks. Wi-Fi was probably suddenly disabled after a scan", npe);
        }
    }

    /**
     * Check which networks should be enabled, and enable them accordingly. Ask for user input when
     * a network's safety level can not be determined
     * @param scanResults The results of the last network scan
     */
    private void checkResults(List<ScanResult> scanResults) {
        // Keep whether the getNetworkSafety function asked the user for input (to know whether we
        // have to disable any notifications afterwards, and to keep the UX as smooth as possible).
        // Alternatively, we would disable previous notifications here, but that would lead to the
        // notification being jittery (disappearing and re-appearing instantly, instead of just
        // updating).
        boolean notificationShown = false;

        // Collect number of found networks, if allowed by user
        /*Analytics analytics = new Analytics(ctx);
        analytics.scanCompleted(scanResults.size());*/

        List<WifiConfiguration> networkList = wifiManager.getConfiguredNetworks();
        if (networkList == null) {
            Log.i("PrivacyPolice", "WifiManager did not return any configured networks. This is "+
                "most likely caused by background location services being allowed to scan for " +
                "Wi-Fi networks, while Wi-Fi is disabled. Keep all networks as before.");
            return;
        }
        // Check for every network in our network list whether it should be enabled
        for (WifiConfiguration network : networkList) {
            AccessPointSafety networkSafety = getNetworkSafety(network, scanResults);
            if (networkSafety == AccessPointSafety.TRUSTED) {
                Log.i("PrivacyPolice", "Enabling " + network.SSID);
                connectTo(network.networkId);
            } else if (networkSafety == AccessPointSafety.UNTRUSTED) {
                // Make sure all other networks are disabled, by disabling them separately
                // (See comment in connectTo() method to see why we don't disable all of them at the
                // same time)
                wifiManager.disableNetwork(network.networkId);
            } else if (networkSafety == AccessPointSafety.UNKNOWN) {
                wifiManager.disableNetwork(network.networkId);
                notificationShown = true;
            }
        }

        if (!notificationShown)
            // Disable previous notifications, to make sure that we only request permission for the
            // currently available networks (and not at the wrong location)
            notificationHandler.disableNotifications();
    }

    /**
     * Enable a given Wi-Fi network, and force Android to connect to it. This function makes sure
     * that connecting also works in Android 5.0 and up.
     * @param networkId The id of the network (found in its configuration) to enable
     */
    private void connectTo(int networkId) {
        // Do not disable other networks, as multiple networks may be available
        wifiManager.enableNetwork(networkId, false);
        // If we aren't already connected to a network, make sure that Android connects.
        // This is required for devices running Android Lollipop (5.0) and up, because
        // they would otherwise never connect.
        wifiManager.reconnect();
        // In some instances (since wpa_supplicant 2.3), even the previous is not sufficient
        // Check if we are in a CONNECTING state, or reassociate to force connection
        Handler handler = new Handler();
        // Wait for 1 second before checking
        handler.postDelayed(new Runnable() {
            public void run() {
                NetworkInfo wifiState = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (!wifiState.isConnectedOrConnecting()) {
                    Log.i("PrivacyPolice", "Reassociating, because WifiManager doesn't seem to be eager to reconnect.");
                    wifiManager.reassociate();
                }
            }
        }, 1000);
    }

    /**
     * Checks whether we should allow connection to a given network, based on the user's preferences
     * It will also ask the user if it is unknown whether the network should be trusted.
     * @param network The network that should be checked
     * @param scanResults The networks that are currently available
     * @return TRUSTED or UNTRUSTED, based on the user's preferences, or UNKNOWN if the user didn't
     *          specify anything yet
     */
    public AccessPointSafety getNetworkSafety(WifiConfiguration network, List<ScanResult> scanResults) {
        // If all settings are disabled by the user, then allow every network
        // This effectively disables all of the app's functionalities
        if (!(prefs.getEnableOnlyAvailableNetworks() || prefs.getOnlyConnectToKnownAccessPoints()))
            return AccessPointSafety.TRUSTED; // Allow every network

        // Always enable hidden networks, since they *need* to use directed probe requests
        // in order to be discovered. Note that using hidden SSID's does not add any
        // real security , so it's best to avoid them whenever possible.
        if (network.hiddenSSID)
            return AccessPointSafety.TRUSTED;

        // Strip double quotes (") from the SSID string
        String plainSSID = network.SSID.substring(1, network.SSID.length() - 1);

        return getNetworkSafety(plainSSID, scanResults);
    }

    /**
     * Checks whether we should allow connection to a given SSID, based on the user's preferences
     * It will also ask the user if it is unknown whether the network should be trusted.
     * @param SSID The SSID of the network that should be checked
     * @param scanResults The networks that are currently available
     * @return TRUSTED or UNTRUSTED, based on the user's preferences, or UNKNOWN if the user didn't
     *          specify anything yet
     */
    public AccessPointSafety getNetworkSafety(String SSID, List<ScanResult> scanResults) {
        for (ScanResult scanResult : scanResults) {
            if (scanResult.SSID.equals(SSID)) {
                // Check whether the user wants to filter by MAC address
                if (!prefs.getOnlyConnectToKnownAccessPoints()) { // Any MAC address is fair game
                    // Enabling now makes sure that we only want to connect when it is in range
                    return AccessPointSafety.TRUSTED;
                } else { // Check access point's MAC address
                    // Check if the MAC address is in the list of allowed MAC's for this SSID
                    Set<String> allowedBSSIDs = prefs.getAllowedBSSIDs(scanResult.SSID);
                    if (allowedBSSIDs.contains(scanResult.BSSID)) {
                        return AccessPointSafety.TRUSTED;
                    } else {
                        // Not an allowed BSSID
                        if (prefs.getBlockedBSSIDs(scanResult.SSID).contains(scanResult.BSSID)) {
                            // This SSID was explicitly blocked by the user!
                            Log.w("PrivacyPolice", "Spoofed network for " + scanResult.SSID + " detected! (BSSID is " + scanResult.BSSID + ")");
                            return AccessPointSafety.UNTRUSTED;
                        } else {
                            // We don't know yet whether the user wants to allow this network
                            // Ask the user what needs to be done
                            notificationHandler.askNetworkPermission(scanResult.SSID, scanResult.BSSID);
                            return AccessPointSafety.UNKNOWN;
                        }
                    }
                }
            }
        }
        return AccessPointSafety.UNTRUSTED; // Network not in range
    }
}
