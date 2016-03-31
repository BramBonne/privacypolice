package be.uhasselt.privacypolice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

/**
 * Activity that shows the user why location access might be needed, and that allows to take action
 * (either open location settings or disable the notification from showing)
 */
public class LocationNoticeActivity extends Activity{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_locationnotice);
    }

    public void openLocationSettings(View view) {
        Intent locationSettingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(locationSettingsIntent);
    }

    public void disableNotification(View view) {
        // TODO
    }
}
