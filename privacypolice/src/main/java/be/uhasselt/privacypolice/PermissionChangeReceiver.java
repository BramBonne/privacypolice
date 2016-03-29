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
    private PreferencesStorage prefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        ctx = context;
        prefs = new PreferencesStorage(ctx);

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
            prefs.addBlockedBSSID(SSID, BSSID);
    }

    private void removeNotification() {
        NotificationHandler notificationHandler = new NotificationHandler(ctx);
        notificationHandler.cancelPermissionRequest();
    }
}
