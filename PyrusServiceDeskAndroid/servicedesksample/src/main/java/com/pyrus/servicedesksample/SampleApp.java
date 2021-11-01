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
            "0HUi7grFuWVFHqWtL3f5YD-4PYJXiOEoLfCDb2yhTthkHBpedNbNU4O01YD2OnsSpvbMiXmweUF8akomZZIW1Ilb-W9mOPuK70L4lCI1mK0dJqXYUp0l-MJlsUv9tr8dSmKCSw==",
            "pyrus.com",
            true
        );

        PyrusServiceDesk.setPushToken(
                "my_push_token",
                exception -> Log.w("SAMPLE_APP", exception));

        PyrusServiceDesk.onAuthorizationFailed(null);
    }
}
