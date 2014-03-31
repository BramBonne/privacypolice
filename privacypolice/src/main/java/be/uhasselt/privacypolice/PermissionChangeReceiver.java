package be.uhasselt.privacypolice;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

public class PermissionChangeReceiver extends BroadcastReceiver {

    private Context ctx;
    private Preferences prefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        ctx = context;
        prefs = new Preferences(ctx);

        boolean enable = intent.getBooleanExtra("enable", true);
        String SSID = intent.getStringExtra("SSID");
        String BSSID = intent.getStringExtra("BSSID");
        Log.d("PrivacyPolice", "Permission change: " + SSID + " " + BSSID + " " + enable);

        removeNotification();

        if (enable) {
            prefs.addAllowedBSSIDsForLocation(SSID);
            // initiate rescan
            WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
            wifiManager.startScan();
        } else
            prefs.addBlockedBSSID(BSSID);
    }

    private void removeNotification() {
        NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(0);
    }
}
