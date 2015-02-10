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
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

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
        FragmentManager mFragmentManager = getFragmentManager();
        FragmentTransaction mFragmentTransaction = mFragmentManager
                .beginTransaction();
        PrefsFragment prefsFragment = new PrefsFragment();
        mFragmentTransaction.replace(R.id.inflatable_prefs, prefsFragment);
        mFragmentTransaction.commit();
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

            // Allow clearing of allowed & blocked APs, via a separate button
            Preference clearHotspotsPreference = findPreference("clearHotspots");
            clearHotspotsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    clearHotspots();
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
         * Ask the user for confirmation that he/she really wants to remove all trusted/untrusted
         * APs.
         */
        public void clearHotspots() {
            // Ask for confirmation first
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.dialog_clearhotspots);
            builder.setPositiveButton(R.string.dialog_clearhotspots_yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Actually clear the list
                    PreferencesStorage prefs = new PreferencesStorage(getActivity());
                    prefs.clearBSSIDLists();
                }
            });
            builder.setNegativeButton(R.string.dialog_clearhotspots_no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User canceled
                }
            });
            builder.show();
        }
    }
}
