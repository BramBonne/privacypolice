package be.uhasselt.privacypolice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

/**
 * Used in Android 5.0 and up, because Location provider needs to be enabled in order to receive
 * Wi-Fi scan results programmatically.
 */
public class LocationAccess extends BroadcastReceiver {
    private LocationManager locationManager;

    public LocationAccess() {
        locationManager = null;
    }

    private void getLocationManager(Context context) {
        if (locationManager == null)
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public boolean isNetworkLocationEnabled(Context context) {
        if (android.os.Build.VERSION.SDK_INT < 21) {
            // Location access is not needed on Android versions < 5.0
            return true;
        }
        getLocationManager(context);
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * Called for intent location.PROVIDERS_CHANGED, BOOT_COMPLETED
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationHandler notificationHandler = new NotificationHandler(context);

        if (!isNetworkLocationEnabled(context)) {
            notificationHandler.askLocationPermission();
        } else
            // Make sure no location permission request is shown
            notificationHandler.cancelLocationPermissionRequest();
    }
}
