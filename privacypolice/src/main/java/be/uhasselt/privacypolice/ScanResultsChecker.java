package be.uhasselt.privacypolice;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.support.v4.app.NotificationCompat;

import java.util.List;
import java.util.Set;

public class ScanResultsChecker extends BroadcastReceiver {
    private static long lastCheck = 0;
    private static Preferences prefs = null;
    private WifiManager wifiManager = null;
    private Context ctx = null;
    private NotificationManager notificationManager = null;

    public void onReceive(Context ctx, Intent i){
        this.ctx = ctx;
        // Older devices might try to scan constantly. Allow them some rest by checking max. once every 0.5 seconds
        if (System.currentTimeMillis() - lastCheck < 500)
            return;
        lastCheck = System.currentTimeMillis();

        // WiFi scan performed
        wifiManager =  (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        prefs = new Preferences(ctx);
        notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        // Disable previous notifications
        notificationManager.cancelAll();

        try {
            List<ScanResult> scanResults = wifiManager.getScanResults();
            Log.d("WiFiPolice", "Wi-Fi scan performed, results are: " + scanResults.toString());

            List<WifiConfiguration> networkList = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration network : networkList) {
                if (isSafe(network, scanResults)) {
                    Log.i("WiFiPolice", "Enabling " + network.SSID);
                    wifiManager.enableNetwork(network.networkId, false); // Do not disable other networks, as multiple networks may be available
                } else
                    wifiManager.disableNetwork(network.networkId); // Make sure all other networks are disabled
            }
        } catch (NullPointerException npe) {
            Log.e("WiFiPolice", "Null pointer exception when handling networks (was Wi-Fi suddenly disabled after a scan?)");
        }
    }

    private boolean isSafe(WifiConfiguration network, List<ScanResult> scanResults) {
        if (!(prefs.getEnableOnlyAvailableNetworks() || prefs.getOnlyConnectToKnownAccessPoints()))
            return true; // Allow every network
        if (network.hiddenSSID)
            return true; // Hidden networks will not show up in scan results

        String plainSSID = network.SSID.substring(1, network.SSID.length() - 1); // Strip "s

        for (ScanResult scanResult : scanResults) {
            if (scanResult.SSID.equals(plainSSID)) {
                if (!prefs.getOnlyConnectToKnownAccessPoints()) { // Any MAC address is fair game
                    // Enabling now makes sure that we only want to connect when it is in range
                    return true;
                } else { // Check access point's MAC address
                    Set<String> allowedBSSIDs = prefs.getAllowedBSSIDs(scanResult.SSID);
                    if (allowedBSSIDs.contains(scanResult.BSSID)) {
                        return true;
                    } else {
                        // Not an allowed BSSID
                        if (prefs.getBlockedBSSIDs().contains(scanResult.BSSID))
                            Log.w("WiFiPolice", "Spoofed network for " + scanResult.SSID + " detected! (BSSID is " + scanResult.BSSID + ")");
                        else
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
        Log.d("WiFiPolice", "Asking permission for " + SSID + " (" + BSSID + ")");
        Intent addIntent = new Intent(ctx, PermissionChangeReceiver.class);
        addIntent.putExtra("SSID", SSID).putExtra("BSSID", BSSID).putExtra("enable", true);
        PendingIntent addPendingIntent = PendingIntent.getBroadcast(ctx, 0, addIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent disableIntent = new Intent(ctx, PermissionChangeReceiver.class);
        disableIntent.putExtra("SSID", SSID).putExtra("BSSID", BSSID).putExtra("enable", false);
        PendingIntent disablePendingIntent = PendingIntent.getBroadcast(ctx, 1, disableIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent activityIntent = new Intent(ctx, AskPermissionActivity.class);
        activityIntent.putExtra("SSID", SSID).putExtra("BSSID", BSSID);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(ctx, 2, activityIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Resources res = ctx.getResources();
        String headerString = String.format(res.getString(R.string.permission_header), SSID);
        String permissionString = String.format(res.getString(R.string.ask_permission), SSID);
        String yes = res.getString(R.string.yes);
        String no = res.getString(R.string.no);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ctx)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(Notification.PRIORITY_MAX) // To force it to be first in list (and thus, expand)
                .setContentTitle(headerString)
                .setContentText(permissionString)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(permissionString))
                .setContentIntent(activityPendingIntent)
                .addAction(android.R.drawable.ic_input_add, yes, addPendingIntent)
                .addAction(android.R.drawable.ic_delete, no, disablePendingIntent);
        notificationManager.notify(0, mBuilder.build());
    }
/*
    public void askSurvey() {
        String lang = Locale.getDefault().getLanguage();
        Log.d("WiFi Police", "Asking to fill in " + lang + " survey.");
        String url;
        if (lang.equals("nl"))
                url = "http://www.google.nl";
        else {
                url = "http://www.google.com";
        }
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

        Resources res = ctx.getResources();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(res.getString(R.string.request_survey_title))
                .setContentText(res.getString(R.string.request_survey_text))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(res.getString(R.string.request_survey_text)))
                .setAutoCancel(true)
                .setContentIntent(pi);
        notificationManager.notify(97, builder.build());
    }
    */
}
