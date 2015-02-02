package be.uhasselt.privacypolice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;
import java.util.Set;

/**
 * This class contains the actual logic for deciding whether a network connection is allowed.
 * It receives the broadcast intents for new scan results, in order to decide whether a network is
 * available. It then checks whether we trust the AP's MAC address, based on the user's configuration.
 */

public class ScanResultsChecker extends BroadcastReceiver {

    private enum AccessPointSafety {
        TRUSTED, UNTRUSTED, UNKNOWN
    }

    private static long lastCheck = 0;
    private static Preferences prefs = null;
    private WifiManager wifiManager = null;
    private NotificationHandler notificationHandler = null;

    /**
     * Called for the following intents:
     *  - SCAN_RESULTS available
     *  - BOOT_COMPLETED
     */
    public void onReceive(Context ctx, Intent i){
        // Older devices might try to scan constantly. Allow them some rest by checking max. once every 0.5 seconds
        if (System.currentTimeMillis() - lastCheck < 500)
            return;
        lastCheck = System.currentTimeMillis();

        // WiFi scan performed
        wifiManager =  (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        prefs = new Preferences(ctx);
        notificationHandler = new NotificationHandler(ctx);

        // Keep whether the getNetworkSafety function asked the user for input (to know whether we
        // have to disable any notifications afterwards, and to keep the UX as smooth as possible).
        // Alternatively, we would disable previous notifications here, but that would lead to the
        // notification being jittery (disappearing and re-appearing instantly, instead of just
        // updating).
        boolean notificationShown = false;

        try {
            List<ScanResult> scanResults = wifiManager.getScanResults();
            Log.d("WiFiPolice", "Wi-Fi scan performed, results are: " + scanResults.toString());

            // Collect number of found networks, if allowed by user
            /*Analytics analytics = new Analytics(ctx);
            analytics.scanCompleted(scanResults.size());*/

            List<WifiConfiguration> networkList = wifiManager.getConfiguredNetworks();
            // Check for every network in our network list whether it should be enabled
            for (WifiConfiguration network : networkList) {
                if (network.hiddenSSID) {
                    // Always enable hidden networks, since they *need* to use directed probe requests
                    // in order to be discovered. Note that using hidden SSID's does not add any
                    // real security , so it's best to avoid them whenever possible.
                    // Do not disable other networks, as multiple networks may be available
                    wifiManager.enableNetwork(network.networkId, false);
                }
                AccessPointSafety networkSafety = getNetworkSafety(network, scanResults);
                if (networkSafety == AccessPointSafety.TRUSTED) {
                    Log.i("WiFiPolice", "Enabling " + network.SSID);
                    // Do not disable other networks, as multiple networks may be available
                    wifiManager.enableNetwork(network.networkId, false);
                    // If we aren't already connected to a network, make sure that Android connects.
                    // This is required for devices running Android Lollipop (5.0) and up, because
                    // they would otherwise never connect.
                    if (!wifiManager.reconnect()) {
                        Log.e("WifiPolice", "Could not reconnect after enabling network");
                    }
                } else if (networkSafety == AccessPointSafety.UNTRUSTED) {
                    // Make sure all other networks are disabled, by disabling them separately
                    // (See previous comment to see why we don't disable all of them at the same
                    // time)
                    wifiManager.disableNetwork(network.networkId);
                } else if (networkSafety == AccessPointSafety.UNKNOWN) {
                    wifiManager.disableNetwork(network.networkId);
                    notificationShown = true;
                }
            }
        } catch (NullPointerException npe) {
            Log.e("WiFiPolice", "Null pointer exception when handling networks. Wi-Fi was probably suddenly disabled after a scan.");
        }

        if (!notificationShown)
            // Disable previous notifications, to make sure that we only request permission for the
            // currently available networks (and not at the wrong location)
            notificationHandler.disableNotifications();
    }

    /**
     * Checks whether we should allow connection to a given network, based on the user's preferences
     * It will also ask the user if it is unknown whether the network should be trusted.
     * @param network The network that should be checked
     * @param scanResults The networks that are currently available
     * @return TRUSTED or UNTRUSTED, based on the user's preferences, or UNKNOWN if the user didn't
     *          specify anything yet
     */
    private AccessPointSafety getNetworkSafety(WifiConfiguration network, List<ScanResult> scanResults) {
        // If all settings are disabled by the user, then allow every network
        // This effectively disables all of the app's functionalities
        if (!(prefs.getEnableOnlyAvailableNetworks() || prefs.getOnlyConnectToKnownAccessPoints()))
            return AccessPointSafety.TRUSTED; // Allow every network

        // Hidden networks will not show up in scan results, keep them enabled at all times
        if (network.hiddenSSID)
            return AccessPointSafety.TRUSTED;

        // Strip double quotes (") from the SSID string
        String plainSSID = network.SSID.substring(1, network.SSID.length() - 1);

        for (ScanResult scanResult : scanResults) {
            if (scanResult.SSID.equals(plainSSID)) {
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
                        if (prefs.getBlockedBSSIDs().contains(scanResult.BSSID)) {
                            // This SSID was explicitly blocked by the user!
                            Log.w("WiFiPolice", "Spoofed network for " + scanResult.SSID + " detected! (BSSID is " + scanResult.BSSID + ")");
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
