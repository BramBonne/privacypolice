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
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/* This activity is used on older devices where actions on notifications are not yet supported.
 * It allows users of those devices to decide whether a network should be trusted.
 * The activity presents itself as a dialog box.
 */

public class AskPermissionActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_askpermission);

        // Get the information for the network that we want to connect to
        Intent intent = getIntent();
        final String SSID = intent.getStringExtra("SSID");
        final String BSSID = intent.getStringExtra("BSSID");

        // Put the name of the network in the notice
        Resources res = getResources();
        String permissionString = String.format(res.getString(R.string.ask_permission), SSID);
        // ... and display it in the correct place
        TextView networkQuestion = (TextView) findViewById(R.id.networkQuestion);
        networkQuestion.setText(permissionString);

        Button yesButton = (Button) findViewById(R.id.yesButton);
        yesButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Notify the permissionChangeReceiver that we want to add a new AP to the trusted list
                        Intent addIntent = new Intent(getApplicationContext(), PermissionChangeReceiver.class);
                        addIntent.putExtra("SSID", SSID).putExtra("BSSID", BSSID).putExtra("enable", true);
                        sendBroadcast(addIntent);
                        finish();
                    }
                }
        );

        Button noButton = (Button) findViewById(R.id.noButton);
        noButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Notify the permissionChangeReceiver that we want to add a new AP to the untrusted list
                        Intent addIntent = new Intent(getApplicationContext(), PermissionChangeReceiver.class);
                        addIntent.putExtra("SSID", SSID).putExtra("BSSID", BSSID).putExtra("enable", false);
                        sendBroadcast(addIntent);
                        finish();
                    }
                }
        );
    }
}
