package net.papirus.servicedesksample;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import net.papirus.pyrusservicedesk.ServiceDeskActivity;

public class SampleActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        findViewById(R.id.support).setOnClickListener(
                view ->
                        startActivity(
                                new ServiceDeskActivity.Builder()
                                        .setStyle(
                                                new ServiceDeskActivity.StyleBuilder()
                                                        .setShowAsDialog(false)
                                                        .setThemeColor(Color.parseColor("#FF8300"))
                                                        .setTitle("AMAZING")
                                                        .setWelcomeMessage("Why so serious?")
                                                        .build())
                                        .build()));
    }
}