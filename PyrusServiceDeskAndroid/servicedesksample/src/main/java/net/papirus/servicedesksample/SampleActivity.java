package net.papirus.servicedesksample;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import net.papirus.pyrusservicedesk.PyrusServiceDesk;
import net.papirus.pyrusservicedesk.ServiceDeskTheme;

public class SampleActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        PyrusServiceDesk.setUser(12345, "man of the hour");
        findViewById(R.id.support).setOnClickListener(
                view -> PyrusServiceDesk.start(
                        this,
                        new ServiceDeskTheme.Builder()
                                .setThemeColor(Color.parseColor("#FF8300"))
                                .setChatTitle("AMAZING")
                                .setWelcomeMessage("Why so serious?")
                                .setAvatarForSupport(new ColorDrawable(Color.BLUE))
                                .build())
        );
    }
}
