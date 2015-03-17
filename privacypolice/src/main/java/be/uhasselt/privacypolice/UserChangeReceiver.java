package be.uhasselt.privacypolice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Makes sure that PrivacyPolice is disabled when the device switches to a different user. This
 * prevents the scenario where PrivacyPolice disables all unavailable networks, which will not be
 * enabled when they become in range again while another user is logged in. If the other user also
 * has PrivacyPolice installed, this user's instance will take over, using the correct
 * configuration for the user.
 * The ACTION_USER_FOREGROUND intent is handled by ScanResultsChecker. This class only disables
 * PrivacyPolice when a switch to a different user account is made.
 */
public class UserChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("PrivacyPolice", "Switching to a different user, enabling all networks");
        WifiManager wifiManager =  (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        // Enable all networks
        for (WifiConfiguration wifiConfiguration : wifiManager.getConfiguredNetworks())
            wifiManager.enableNetwork(wifiConfiguration.networkId, false);
    }
}
