package com.pyrus.servicedesksample;

import android.app.Application;
import android.util.Log;

import com.pyrus.pyrusservicedesk.PyrusServiceDesk;

public class SampleApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PyrusServiceDesk.init(
                this,
                "my_app_id",
                "user_id",
                "security_key"
        );

        PyrusServiceDesk.setPushToken(
                "my_push_token",
                exception -> Log.w("SAMPLE_APP", exception));

        PyrusServiceDesk.setOnAuthorizationFailed(null);
    }
}
