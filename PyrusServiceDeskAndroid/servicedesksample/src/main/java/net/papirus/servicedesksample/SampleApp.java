package net.papirus.servicedesksample;

import android.app.Application;
import android.util.Log;
import net.papirus.pyrusservicedesk.PyrusServiceDesk;

public class SampleApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PyrusServiceDesk.init(
                this,
                "my_app_id"
        );

        PyrusServiceDesk.setPushToken(
                "my_push_token",
                exception -> {
                    Log.d("SAMPLE_APP", exception.getMessage());
                });

    }
}
