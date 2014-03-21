package be.uhasselt.privacypolice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScanResultsChecker extends BroadcastReceiver {
    private static long lastCheck = 0;
    private static Preferences prefs = null;
    private WifiManager wifiManager = null;

    public void onReceive(Context ctx, Intent i){
        // Older devices might try to scan constantly. Allow them some rest by checking max. once every 5 seconds
        if (System.currentTimeMillis() - lastCheck < 5000)
            return;
        lastCheck = System.currentTimeMillis();
        // WiFi scan performed
        wifiManager =  (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        prefs = new Preferences(ctx);

        if (prefs.getEnableOnlyAvailableNetworks())
            enableOnlyAvailableNetworks();
    }

    public void enableOnlyAvailableNetworks() {
        List<ScanResult> scanResults = wifiManager.getScanResults();
        Set<String> scanSSIDs = new HashSet<String>();
        for (ScanResult scanResult : scanResults) {
            scanSSIDs.add(scanResult.SSID);
        }
        Log.d("WiFiPolice", "Wi-Fi scan performed, results are: " + scanSSIDs.toString());
        List<WifiConfiguration> networkList = wifiManager.getConfiguredNetworks();

        for (WifiConfiguration network : networkList) {
            String plainSSID = network.SSID.substring(1, network.SSID.length() - 1); // Strip "s
            if (scanSSIDs.contains(plainSSID)) {
                Log.d("WiFiPolice", "Enabling " + network.SSID);
                wifiManager.enableNetwork(network.networkId, true); // Also disable other networks
                return; // We're done (assuming we are able to connect to the network).
            } else {
                // Make sure network is disabled (otherwise, the last connected network will remain enabled forever)
                wifiManager.disableNetwork(network.networkId);
            }
        }
    }
}
