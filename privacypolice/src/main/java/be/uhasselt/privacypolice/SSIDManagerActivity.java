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
 **/

package be.uhasselt.privacypolice;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.net.wifi.ScanResult;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Activity that allows the user to view and modify the stored list of allowed / blocked networks.
 * This activity contains only a list of the networks, and the option (in the menu) to remove
 * all stored networks.
 */

public class SSIDManagerActivity extends ListActivity {
    private ListAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new SSIDAdapter(this);
        setListAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        // This menu contains only one item: the removal of all networks altogether
        inflater.inflate(R.menu.ssidmanager, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_removeall:
                // Ask the user to confirm that he/she wants to remove all networks
                clearHotspots();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Ask the user for confirmation that he/she really wants to remove all trusted/untrusted
     * APs.
     */
    public void clearHotspots() {
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

    /**
     * Adapter that is responsible for populating the list of networks. In this case, the adapter
     * also contains all logic to sort the networks by availability, and for getting the list from
     * the preference storage.
     */
    private class SSIDAdapter extends BaseAdapter {
        private PreferencesStorage prefs = null;
        private WifiManager wifiManager = null;
        private LayoutInflater layoutInflater = null;
        // Store the list of SSIDs we know, together with their current availability
        private ArrayList<SSID> ssidList = null;

        public SSIDAdapter(Context ctx) {
            prefs = new PreferencesStorage(ctx);
            wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
            layoutInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            // Create the list for the first time
            refresh();
        }

        /**
         * Repopulate the list by getting the latest information on available networks, and
         * combining them by networks stored in the preferences.
         * Only displays networks that are stored in the preferences.
         */
        public void refresh() {
            Log.v("PrivacyPolice", "Refreshing the SSID adapter");
            // Use an ArrayMap so we can put available networks at the top
            ssidList = new ArrayList<>();

            // Combine the SSIDs that we know of with the SSIDs that are available.
            List<ScanResult> scanResults = wifiManager.getScanResults();
            Set<String> knownSSIDs = prefs.getKnownSSIDs();

            // Add currently available networks that are stored in the preferences to the list
            for (ScanResult scanResult : scanResults) {
                if (knownSSIDs.contains(scanResult.SSID)) {
                    ssidList.add(new SSID(scanResult.SSID, true));
                    knownSSIDs.remove(scanResult.SSID);
                }
            }

            // Add all other (non-available) saved SSIDs to the list
            for (String SSID : knownSSIDs) {
                ssidList.add(new SSID(SSID, false));
            }
        }

        @Override
        public int getCount() {
            return ssidList.size();
        }

        @Override
        public Object getItem(int position) {
            return ssidList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * Get the layout for list item at position 'position'
         * @param position the position in the list
         * @param convertView a previously created view (if available)
         * @param parent the parent view
         * @return the layout that can be used in the list
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout layout;
            // Recycle a previous view, if available
            if (convertView == null) {
                // Not available, create a new view
                layout = (LinearLayout) layoutInflater.inflate(R.layout.item_ssidmanager, null);
            } else {
                layout = (LinearLayout) convertView;
            }

            // Fill in the text part of the layout with the SSID
            SSID SSIDinfo = (SSID) getItem(position);
            TextView SSIDtext = (TextView) layout.findViewById(R.id.SSIDname);
            SSIDtext.setText(SSIDinfo.getName());
            // Make the 'signal strength' icon visible if the network is available
            ImageView signalStrengthImage = (ImageView) layout.findViewById(R.id.signalStrength);
            if (SSIDinfo.isAvailable()) {
                signalStrengthImage.setVisibility(View.VISIBLE);
            } else {
                signalStrengthImage.setVisibility(View.INVISIBLE);
            }

            return layout;
        }
    }

    /**
     * Helper class used for storing an SSID together with whether the network is currently
     * available.
     */
    private class SSID {
        private String name;
        private boolean available;

        private SSID(String name, boolean available) {
            this.setName(name);
            this.setAvailable(available);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }
    }
}
