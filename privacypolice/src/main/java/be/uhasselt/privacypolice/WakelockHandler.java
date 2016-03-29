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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * This class makes sure that AppStandby on Android 6.0 and up does not interfere with PrivacyPolice
 * (and thus, with normal Wi-Fi operation) by scheduling conservative wakelocks/alarms regularly.
 * This prevents the kind of situation where all apps that need Wi-Fi access are woken up regularly
 * by the OS, whereas PrivacyPolice (having no Activities that are visited regularly by the user)
 * is kept idle, preventing the results of a scan from being passed on to to the
 * PermissionChangeReceiver, and preventing Wi-Fi access for the entire OS.
 * It works by triggering an alarm approximately every 15 minutes, while still adhering to Android's
 * doze mode.
 * TODO: check if this is still needed after the location services fix
 */
public class WakelockHandler extends BroadcastReceiver {
    private static WakelockHandler instance = null;
    private Context context = null;
    private AlarmManager alarmManager = null;

    public static WakelockHandler getInstance(Context ctx) {
        if (instance == null) {
            instance = new WakelockHandler(ctx);
        }
        return instance;
    }

    public WakelockHandler(Context ctx) {
        this.context = ctx;
        this.alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        ensureAwake();
    }

    public void scheduleAlarms() {
        Intent wakeupIntent = new Intent(context, WakelockHandler.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, this.hashCode(), wakeupIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, AlarmManager.INTERVAL_FIFTEEN_MINUTES, pendingIntent);
    }

    public void ensureAwake() {
        Log.v("PrivacyPolice", "Ensuring we're still awake");
        Intent wakeupIntent = new Intent(context, WakelockHandler.class);
        // Check if the alarm is still scheduled
        if (PendingIntent.getBroadcast(context, this.hashCode(), wakeupIntent, PendingIntent.FLAG_NO_CREATE) == null) {
            Log.w("PrivacyPolice", "Re-scheduling alarms, since our alarm manager didn't seem to be running");
            scheduleAlarms();
        }
    }

    public WakelockHandler() {
        // Only used for the BroadcastReceiver aspect of this class
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("PrivacyPolice", "Waking up because of alarm");
        WifiManager wifiManager =  (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();
    }
}
