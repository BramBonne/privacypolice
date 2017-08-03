package be.uhasselt.privacypolice;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

/**
 * Activity that shows the user why location access might be needed, and that allows to take action
 * (either open location settings or disable the notification from showing)
 */
public class LocationNoticeActivity extends Activity{
    public static int PERMISSION_REQUEST_CODE = 10;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_locationnotice);

        // Display the location preferences fragment
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        PrefsFragment prefsFragment = new PrefsFragment();
        fragmentTransaction.replace(R.id.inflatable_locationprefs, prefsFragment);
        fragmentTransaction.commit();
    }

    public void checkAndRequestSettings(View view) {
        // First, check if we need to request the location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted yet, display request
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
        else {
            // Permission has been granted already, go straight to the location settings
            openLocationSettings();
        }
    }

    private void openLocationSettings() {
        Intent locationSettingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(locationSettingsIntent);
    }

    private void openAppSettings() {
        Intent appSettingsIntent = new Intent();
        appSettingsIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri packageName = Uri.fromParts("package", getPackageName(), null);
        appSettingsIntent.setData(packageName);
        startActivity(appSettingsIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST_CODE) {
            Log.e("PrivacyPolice", "Permissions result with unexpected request code: " + requestCode);
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i("PrivacyPolice", "User denied location access");
            // Let the user enable this permission through the settings, because he may have ticked
            // the 'do not ask me again' box.
            openAppSettings();
        } else {
            // Check if we also need the user to enable location on their phone
            if (!LocationAccess.isNetworkLocationEnabled(this)) {
                openLocationSettings();
            }
        }
        // Re-check if notification should be displayed
        LocationAccess.checkAccessDisplayNotification(this);
    }

    /**
     * Fragment that is automatically filled with all preferences described in xml/preferences_location.xml
     */
    public static class PrefsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences_location);
        }
    }
}
