package be.uhasselt.privacypolice;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Activity that allows the user to view and modify the stored list of allowed / blocked networks.
 * This activity contains only a list of the SSIDs, and the option (in the menu) to remove
 * all stored SSIDs.
 **/
public class SSIDManagerActivity extends NetworkManagerActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v("PrivacyPolice", "Creating SSID manager activity");
        super.onCreate(savedInstanceState);

        adapter = new SSIDManagerAdapter();
        setListAdapter(adapter);
    }

    @Override
    public void confirmClearAll() {
        // Ask for confirmation first
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_clearhotspots);
        builder.setPositiveButton(R.string.dialog_clearhotspots_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Actually clear the list
                PreferencesStorage prefs = new PreferencesStorage(SSIDManagerActivity.this);
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

    protected class SSIDManagerAdapter extends NetworkManagerAdapter {
        public void refresh() {
            Log.v("PrivacyPolice", "Refreshing the network list adapter");
            // Use an ArrayMap so we can put available networks at the top
            networkList = new ArrayList<>();

            // Combine the SSIDs that we know of with the SSIDs that are available.
            List<ScanResult> scanResults = wifiManager.getScanResults();
            Set<String> knownSSIDs = prefs.getKnownSSIDs();

            // Add currently available networks that are stored in the preferences to the list
            for (ScanResult scanResult : scanResults) {
                if (knownSSIDs.contains(scanResult.SSID)) {
                    networkList.add(new NetworkAvailability(scanResult.SSID, true));
                    knownSSIDs.remove(scanResult.SSID);
                }
            }

            // Add all other (non-available) saved SSIDs to the list
            for (String SSID : knownSSIDs) {
                networkList.add(new NetworkAvailability(SSID, false));
            }
        }
    }
}
