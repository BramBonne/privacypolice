package be.uhasselt.privacypolice;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class MainActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        /* Now bound in manifest
        // Bind the ScanResultsChecker to an intent filter listening for new Wi-Fi scans
        IntentFilter i = new IntentFilter();
        i.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(new ScanResultsChecker(getApplicationContext()), i );*/
    }


}
