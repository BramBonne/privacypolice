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
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        inflater.inflate(R.menu.ssidmanager, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_removeall:
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

            refresh();
        }

        public void refresh() {
            Log.v("PrivacyPolice", "Refreshing the SSID adapter");
            // Use an ArrayMap so we can put available networks at the top
            ssidList = new ArrayList<>();

            List<ScanResult> scanResults = wifiManager.getScanResults();
            Set<String> knownSSIDs = prefs.getKnownSSIDs();

            // Add currently available networks that we (at least partially) trust to the list
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
            // TODO: Check if it is needed that we implement this function
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView;
            if (convertView == null) { // We cannot recycle a previous view
                textView = (TextView) layoutInflater.inflate(R.layout.item_ssidmanager, null);
            } else {
                textView = (TextView) convertView;
            }

            SSID SSIDinfo = ssidList.get(position);
            textView.setText(SSIDinfo.getName());
            Log.v("PrivacyPolice", "Adding new SSID to manager list: " + textView.getText());
            if (SSIDinfo.isAvailable()) {
                // TODO: Make it look enabled
            } else {
                // TODO: Make it look disabled
                //textView.setBackgroundColor(R.co);
            }

            return textView;
        }
    }

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
