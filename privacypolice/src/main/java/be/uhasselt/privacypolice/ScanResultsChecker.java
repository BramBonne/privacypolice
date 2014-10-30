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

        // Disable previous notifications, to make sure that we only request permission for the currently
        // available networks
        notificationHandler.disableNotifications();

        try {
            List<ScanResult> scanResults = wifiManager.getScanResults();
            Log.d("WiFiPolice", "Wi-Fi scan performed, results are: " + scanResults.toString());

            List<WifiConfiguration> networkList = wifiManager.getConfiguredNetworks();
            // Check for every network in our network list whether it should be enabled
            for (WifiConfiguration network : networkList) {
                if (isSafe(network, scanResults)) {
                    Log.i("WiFiPolice", "Enabling " + network.SSID);
                    // Do not disable other networks, as multiple networks may be available
                    wifiManager.enableNetwork(network.networkId, false);
                } else {
                    // Make sure all other networks are disabled, by disabling them separately
                    // (See previous comment to see why we don't disable all of them at the same
                    // time)
                    wifiManager.disableNetwork(network.networkId);
                }
            }
        } catch (NullPointerException npe) {
            Log.e("WiFiPolice", "Null pointer exception when handling networks. Wi-Fi was probably suddenly disabled after a scan.");
        }
    }

    /**
     * Checks whether we should allow connection to a given network, based on the user's preferences
     * @param network The network that should be checked
     * @param scanResults The networks that are currently available
     * @return True if the network may be enabled
     */
    private boolean isSafe(WifiConfiguration network, List<ScanResult> scanResults) {
        // If all settings are disabled by the user, then allow every network
        // This effectively disables all of the app's functionalities
        if (!(prefs.getEnableOnlyAvailableNetworks() || prefs.getOnlyConnectToKnownAccessPoints()))
            return true; // Allow every network

        // Hidden networks will not show up in scan results, keep them enabled at all times
        if (network.hiddenSSID)
            return true;

        // Strip double quotes (") from the SSID string
        String plainSSID = network.SSID.substring(1, network.SSID.length() - 1);

        for (ScanResult scanResult : scanResults) {
            if (scanResult.SSID.equals(plainSSID)) {
                // Check whether the user wants to filter by MAC address
                if (!prefs.getOnlyConnectToKnownAccessPoints()) { // Any MAC address is fair game
                    // Enabling now makes sure that we only want to connect when it is in range
                    return true;
                } else { // Check access point's MAC address
                    // Check if the MAC address is in the list of allowed MAC's for this SSID
                    Set<String> allowedBSSIDs = prefs.getAllowedBSSIDs(scanResult.SSID);
                    if (allowedBSSIDs.contains(scanResult.BSSID)) {
                        return true;
                    } else {
                        // Not an allowed BSSID
                        if (prefs.getBlockedBSSIDs().contains(scanResult.BSSID)) {
                            // This SSID was explicitly blocked by the user!
                            Log.w("WiFiPolice", "Spoofed network for " + scanResult.SSID + " detected! (BSSID is " + scanResult.BSSID + ")");
                        } else {
                            // We don't know yet whether the user wants to allow this network
                            // Ask the user what needs to be done
                            notificationHandler.askNetworkPermission(scanResult.SSID, scanResult.BSSID);
                        }
                        // Block temporarily / permanently depending on the previous if-test
                        return false;
                    }
                }
            }
        }
        return false; // Network not in range
    }
}
