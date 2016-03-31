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

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * Since PrivacyPolice does not need a real MainActivity, this class is used to modify the
 * preferences, and view the state of the application.
 */
public class PreferencesActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preferences);

        // Display the preferences fragment as the main content.
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        PrefsFragment prefsFragment = new PrefsFragment();
        fragmentTransaction.replace(R.id.inflatable_prefs, prefsFragment);
        fragmentTransaction.commit();

        // Show the location notice if location is disabled
        TextView locationNotice = (TextView) findViewById(R.id.location_notice);
        LocationAccess locationAccess = new LocationAccess();
        if (!locationAccess.isNetworkLocationEnabled(getApplicationContext()))
            locationNotice.setVisibility(View.VISIBLE);
        else
            locationNotice.setVisibility(View.GONE);
    }

    /**
     * Called when location notification is clicked
     */
    public void openLocationNotice(View view) {
        Intent intent = new Intent(this, LocationNoticeActivity.class);
        startActivity(intent);
    }

    /**
     * Fragment that is automatically filled with all preferences described in xml/preferences.xml
     */
    public static class PrefsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
            try {
                SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
                prefs.registerOnSharedPreferenceChangeListener(this);
            } catch (NullPointerException npe) {
                Log.e("PrivacyPolice", "Null pointer exception when trying to register shared preference change listener");
            }

            // Allow modifying of allowed & blocked APs, via a separate button
            Preference modifyHotspotsPreference = findPreference("modifyHotspots");
            modifyHotspotsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    modifyHotspots();
                    return true;
                }
            });
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        {
            // Perform a rescan every time a preference has changed
            Log.v("PrivacyPolice", "Initiating rescan because preference " + key + " changed");
            try {
                WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
                wifiManager.startScan();
            } catch (NullPointerException npe) {
                Log.e("PrivacyPolice", "Could not get WifiManager from within prefsFragment");
            }
        }

        /**
         * Launch the SSID manager activity
         */
        public void modifyHotspots() {
            Log.v("PrivacyPolice", "Launching SSID manager");
            Intent intent = new Intent(getActivity(), SSIDManagerActivity.class);
            startActivity(intent);
        }
    }
}
