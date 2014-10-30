package be.uhasselt.privacypolice;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Class used to show notifications to the user, and to ask permissions
 */
public class NotificationHandler {
    Context ctx = null;
    NotificationManager notificationManager = null;

    NotificationHandler(Context ctx) {
        this.ctx = ctx;
        notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Disables all currently displayed notifications by PrivacyPolice. This allows us to remove
     * stale networks.
     */
    public void disableNotifications() {
        notificationManager.cancelAll();
    }

    /**
     * Asks the user whether it is certain that a network should be currently available
     * @param SSID The name of the network
     * @param BSSID The MAC address of the access point that triggered this (only used when we will block the AP)
     */
    public void askNetworkPermission(String SSID, String BSSID) {
        Log.d("WiFiPolice", "Asking permission for " + SSID + " (" + BSSID + ")");
        // Intent that will be used when the user allows the network
        Intent addIntent = new Intent(ctx, PermissionChangeReceiver.class);
        addIntent.putExtra("SSID", SSID).putExtra("BSSID", BSSID).putExtra("enable", true);
        PendingIntent addPendingIntent = PendingIntent.getBroadcast(ctx, 0, addIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        // Intent that will be used when the user blocks the network
        Intent disableIntent = new Intent(ctx, PermissionChangeReceiver.class);
        disableIntent.putExtra("SSID", SSID).putExtra("BSSID", BSSID).putExtra("enable", false);
        PendingIntent disablePendingIntent = PendingIntent.getBroadcast(ctx, 1, disableIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        // Intent that will be used when the user's OS does not support notification actions
        Intent activityIntent = new Intent(ctx, AskPermissionActivity.class);
        activityIntent.putExtra("SSID", SSID).putExtra("BSSID", BSSID);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(ctx, 2, activityIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        // Build the notification dynamically, based on the network name
        Resources res = ctx.getResources();
        String headerString = String.format(res.getString(R.string.permission_header), SSID);
        String permissionString = String.format(res.getString(R.string.ask_permission), SSID);
        String yes = res.getString(R.string.yes);
        String no = res.getString(R.string.no);

        // NotificationCompat makes sure that the notification will also work on Android <4.0
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
    // This code will be used in the future, when we want to ask the user about his/her experience
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
