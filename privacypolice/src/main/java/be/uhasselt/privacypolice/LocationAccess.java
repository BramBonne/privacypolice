package be.uhasselt.privacypolice;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

/**
 * Used in Android 6.0 and up, because Location provider needs to be enabled in order to receive
 * Wi-Fi scan results programmatically.
 */
public class LocationAccess extends BroadcastReceiver {
    public static boolean isNetworkLocationEnabled(Context context) {
        if (android.os.Build.VERSION.SDK_INT < 23) {
            // Location access is not needed on Android versions < 6.0
            // See https://code.google.com/p/android/issues/detail?id=185370 for more information
            return true;
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("PrivacyPolice", "I don't seem to have the correct runtime permission!");
            return false;
        }
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * Called for intent location.PROVIDERS_CHANGED, BOOT_COMPLETED
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        checkAccessDisplayNotification(context);
    }

    public static void checkAccessDisplayNotification(Context context) {
        NotificationHandler notificationHandler = new NotificationHandler(context);

        if (!isNetworkLocationEnabled(context)) {
            notificationHandler.askLocationPermission();
        } else {
            // Make sure no location permission request is shown
            notificationHandler.cancelLocationPermissionRequest();
        }
    }
}
