package be.uhasselt.privacypolice;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Class that handles user actions deciding whether or not an AP should be trusted. Broadcasts are
 * sent out by NotificationHandler and AskPermissionActivity.
 */
public class PermissionChangeReceiver extends BroadcastReceiver {

    private Context ctx;
    private Preferences prefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        ctx = context;
        prefs = new Preferences(ctx);

        // Remove the notification that was used to make the decision
        removeNotification();

        boolean enable = intent.getBooleanExtra("enable", true);
        String SSID = intent.getStringExtra("SSID");
        String BSSID = intent.getStringExtra("BSSID");

        if (SSID == null || BSSID == null) {
            Log.e("PrivacyPolice", "Could not set permission because SSID or BSSID was null!");
            return;
        }

        Log.d("PrivacyPolice", "Permission change: " + SSID + " " + BSSID + " " + enable);

        if (enable) {
            prefs.addAllowedBSSIDsForLocation(SSID);
            // initiate rescan, to make sure our algorithm enables the network, and to make sure
            // that Android connects to it
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
