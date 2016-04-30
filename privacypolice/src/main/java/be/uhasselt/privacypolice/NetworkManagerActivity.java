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

import android.app.ListActivity;
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
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Activity that allows the user to view and modify the stored list of allowed / blocked networks.
 * This activity contains only a list of the networks, and the option (in the menu) to remove
 * all stored networks.
 * This activity is subclassed by SSIDManagerActivity and MACManagerActivity, to manage respectively
 * the list of networks, and the list of access points for a specific network.
 */

public abstract class NetworkManagerActivity extends ListActivity {
    protected NetworkManagerAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * When bringing this activity to the foreground, refresh the network list.
     */
    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    /**
     * Provide the option on the top right of the screen to remove all remembered networks
     * @param menu The options menu (provided by the Android OS)
     * @return success
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        // This menu contains only one item: the removal of all networks altogether
        inflater.inflate(R.menu.networkmanager, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_removeall:
                // Ask the user to confirm that he/she wants to remove all networks
                confirmClearAll();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Repopulate the list by getting the latest information on available networks, and
     * combining them with networks stored in the preferences.
     */
    public void refresh() {
        adapter.refresh();
    }

    /**
     * Ask the user for confirmation that he/she really wants to remove all trusted/untrusted
     * APs, and remove them if the user confirms.
     */
    public abstract void confirmClearAll();

    /**
     * Adapter that is responsible for populating the list of networks. In this case, the adapter
     * also contains all logic to sort the networks by availability, and for getting the list from
     * the preference storage.
     */
    protected abstract class NetworkManagerAdapter extends BaseAdapter {
        protected PreferencesStorage prefs = null;
        protected WifiManager wifiManager = null;
        private LayoutInflater layoutInflater = null;
        // Store the list of networks we know, together with their current availability
        protected ArrayList<NetworkAvailability> networkList = null;

        public NetworkManagerAdapter() {
            Context context = NetworkManagerActivity.this.getApplicationContext();
            prefs = new PreferencesStorage(context);
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            // Creating the list for the first time
            refresh();
        }

        /**
         * Repopulate the list by getting the latest information on available networks, and
         * combining them with networks stored in the preferences.
         * Only displays networks that are stored in the preferences.
         */
        public abstract void refresh();

        @Override
        public int getCount() {
            return networkList.size();
        }

        @Override
        public Object getItem(int position) {
            return networkList.get(position);
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
                layout = (LinearLayout) layoutInflater.inflate(R.layout.item_networkmanager, null);
            } else {
                layout = (LinearLayout) convertView;
            }

            // Fill in the text part of the layout with the NetworkAvailability
            NetworkAvailability SSIDinfo = (NetworkAvailability) getItem(position);
            TextView SSIDtext = (TextView) layout.findViewById(R.id.SSIDname);
            SSIDtext.setText(SSIDinfo.getName());
            // Make the 'signal strength' icon visible if the network is available
            ImageView signalStrengthImage = (ImageView) layout.findViewById(R.id.signalStrength);
            Log.v("PrivacyPolice", "Adding network " + SSIDinfo.getName() + " with signal strength " + SSIDinfo.getSignalStrength());
            // Color signal strength teal (if trusted) or pink (if blocked)
            String color = "teal";
            if (SSIDinfo.getAccessPointSafety() == ScanResultsChecker.AccessPointSafety.UNTRUSTED)
                color = "pink";
            String resourceName = "ic_wifi_signal_" + SSIDinfo.getSignalStrength() + "_" + color;
            if (SSIDinfo.getSignalStrength() == -1)
                resourceName = "ic_wifi_unavailable_" + color;
            int resourceId = getResources().getIdentifier(resourceName, "drawable", getPackageName());
            signalStrengthImage.setImageResource(resourceId);

            return layout;
        }
    }

    /**
     * Helper class used for storing a network together with its safety (according to PrivacyPolice)
     * and its current signal strength
     */
    protected class NetworkAvailability {
        private String name;
        private int signalStrength;
        private ScanResultsChecker.AccessPointSafety accessPointSafety;

        public NetworkAvailability(String name, int rssi, ScanResultsChecker.AccessPointSafety accessPointSafety) {
            this.setName(name);
            this.setSignalStrength(rssi);
            this.setAccessPointSafety(accessPointSafety);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isAvailable() {
            return signalStrength >= 0;
        }

        public int getSignalStrength() {
            return signalStrength;
        }

        public void setSignalStrength(int rssi) {
            if (rssi < -999) {
                signalStrength = -1;
            } else {
                // Map the RSSI value to an integer in the range [0,4]
                signalStrength = WifiManager.calculateSignalLevel(rssi, 5);
            }
        }

        public ScanResultsChecker.AccessPointSafety getAccessPointSafety() {
            return accessPointSafety;
        }

        public void setAccessPointSafety(ScanResultsChecker.AccessPointSafety accessPointSafety) {
            this.accessPointSafety = accessPointSafety;
        }
    }
}
