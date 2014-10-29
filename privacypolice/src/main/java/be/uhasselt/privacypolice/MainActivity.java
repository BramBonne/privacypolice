package be.uhasselt.privacypolice;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

public class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Display the fragment as the main content.
        FragmentManager mFragmentManager = getFragmentManager();
        FragmentTransaction mFragmentTransaction = mFragmentManager
                .beginTransaction();
        PrefsFragment prefsFragment = new PrefsFragment();
        mFragmentTransaction.replace(R.id.inflatable_prefs, prefsFragment);
        mFragmentTransaction.commit();

        /* Now bound in manifest
        // Bind the ScanResultsChecker to an intent filter listening for new Wi-Fi scans
        IntentFilter i = new IntentFilter();
        i.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(new ScanResultsChecker(getApplicationContext()), i );*/
    }

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

            // Allow clearing of allowed & blocked networks
            Preference clearHotspotsPreference = findPreference("clearHotspots");
            clearHotspotsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // TODO: ask for confirmation
                    Log.d("PrivacyPolice", "Removing all trusted/untrusted hotspots");
                    Preferences prefs = new Preferences(getActivity().getApplicationContext());
                    prefs.clearBSSIDLists();
                    return true;
                }
            });
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        {
            // Perform a rescan
            Log.v("PrivacyPolice", "Initiating rescan because preference " + key + " changed");
            try {
                WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
                wifiManager.startScan();
            } catch (NullPointerException npe) {
                Log.e("PrivacyPolice", "Could not get WifiManager from within prefsFragment");
            }
        }
    }
}
