package be.uhasselt.privacypolice;

/**
 * Used for sending usage data. No personal information is collected, and data is carefully
 * anonymized before being used *for research purposes only*
 * This code is currently not used, but might be used for future research.
 */

import android.content.Context;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class Analytics {
    private Tracker tracker;
    private Context context;

    public Analytics(Context ctx) {
        context = ctx;
        // If the user does not want to / is not able to be tracked, do not create an Analytics instance
        if (!isTrackingEnabled())
            return;
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(ctx);
        // Use id = 0, since we will be using only one analytics tracker
        tracker = analytics.newTracker(R.xml.tracker);
    }

    public boolean isTrackingEnabled() {
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS) {
            Log.d("PrivacyPolice", "Not using analytics because Google Play services is disabled");
            return false;
        }
        PreferencesStorage prefs = new PreferencesStorage(context);
        if (!prefs.getTrackingAllowed()) {
            Log.d("PrivacyPolice", "Not using analytics because the user has indicated he/she does not want to be tracked");
            return false;
        }
        return true;
    }

    public void scanCompleted(int nAccessPoints) {
        if (!isTrackingEnabled())
            return;
        Log.v("PrivacyPolice", "Sending analytics data about a completed scan");
        tracker.send(new HitBuilders.EventBuilder()
            .setCategory("Tech")
            .setAction("accessPointsPerScan")
            .setValue(nAccessPoints)
            .build()
        );
    }
}
