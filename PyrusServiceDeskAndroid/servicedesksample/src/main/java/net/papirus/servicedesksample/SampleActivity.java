package net.papirus.servicedesksample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import net.papirus.pyrusservicedesk.ServiceDeskActivity;

public class SampleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        findViewById(R.id.support).setOnClickListener(
                view ->
                        startActivity(ServiceDeskActivity.createIntent()));
    }
}
