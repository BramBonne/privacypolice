package be.uhasselt.privacypolice;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class AskPermissionActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_askpermission);

        Intent intent = getIntent();
        final String SSID = intent.getStringExtra("SSID");
        final String BSSID = intent.getStringExtra("BSSID");

        Resources res = getResources();
        String permissionString = String.format(res.getString(R.string.ask_permission), SSID);

        TextView networkQuestion = (TextView) findViewById(R.id.networkQuestion);
        networkQuestion.setText(permissionString);

        Button yesButton = (Button) findViewById(R.id.yesButton);
        yesButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
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
                        Intent addIntent = new Intent(getApplicationContext(), PermissionChangeReceiver.class);
                        addIntent.putExtra("SSID", SSID).putExtra("BSSID", BSSID).putExtra("enable", false);
                        sendBroadcast(addIntent);
                        finish();
                    }
                }
        );
    }

}
