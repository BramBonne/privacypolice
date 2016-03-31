package be.uhasselt.privacypolice;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

/**
 * Activity that shows the user why location access might be needed, and that allows to take action
 * (either open location settings or disable the notification from showing)
 */
public class LocationNoticeActivity extends Activity{
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

    public void openLocationSettings(View view) {
        Intent locationSettingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(locationSettingsIntent);
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
