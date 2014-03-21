package be.uhasselt.privacypolice;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ScanResultsChecker extends BroadcastReceiver {
    private static long lastCheck = 0;
    private static Preferences prefs = null;
    private WifiManager wifiManager = null;
    private Context ctx = null;

    public void onReceive(Context ctx, Intent i){
        this.ctx = ctx;
        // Older devices might try to scan constantly. Allow them some rest by checking max. once every 5 seconds
        if (System.currentTimeMillis() - lastCheck < 5000)
            return;
        lastCheck = System.currentTimeMillis();
        // WiFi scan performed
        wifiManager =  (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        prefs = new Preferences(ctx);

        List<WifiConfiguration> enabledNetworks = null;
        if (!(prefs.getEnableOnlyAvailableNetworks() || prefs.getOnlyConnectToKnownAccessPoints()))
            // Nothing to do
            return;

        List<ScanResult> scanResults = wifiManager.getScanResults();
        Log.d("WiFiPolice", "Wi-Fi scan performed, results are: " + scanResults.toString());

        List<WifiConfiguration> networkList = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration network : networkList) {
            if (isSafe(network, scanResults)) {
                Log.d("WiFiPolice", "Enabling " + network.SSID);
                wifiManager.enableNetwork(network.networkId, false); // Do not disable other networks, as multiple networks may be available
            } else
                wifiManager.disableNetwork(network.networkId); // Make sure all other networks are disabled
        }
    }

    private boolean isSafe(WifiConfiguration network, List<ScanResult> scanResults) {
        HashMap<String, Set> allowedBSSIDs = prefs.getAllowedBSSIDs();
        String plainSSID = network.SSID.substring(1, network.SSID.length() - 1); // Strip "s

        for (ScanResult scanResult : scanResults) {
            if (scanResult.SSID.equals(plainSSID)) {
                if (!prefs.getOnlyConnectToKnownAccessPoints()) { // Any BSSID is fair game
                    return true;
                } else { // Check BSSID
                    Log.d("WiFiPolice", "Will enable " + network.SSID + " if we have already seen its BSSID.");
                    if (allowedBSSIDs.containsKey(scanResult.SSID) && allowedBSSIDs.get(scanResult.SSID).contains(scanResult.BSSID)) {
                        return true;
                    } else {
                        // Not an allowed BSSID
                        // Allow the user to add it to the whitelist
                        askNetworkPermission(scanResult.SSID, scanResult.BSSID);
                        // Block temporarily
                        return false;
                    }
                }
            }
        }
        return false; // Network not in range
    }

    public void askNetworkPermission(String SSID, String BSSID) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ctx)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("New network encountered")
                .setContentText("Are you sure the network \"" + SSID + "\" should be available right now?");
        NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mBuilder.build());
    }
}
