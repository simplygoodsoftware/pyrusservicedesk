package com.pyrus.servicedesksample;

import android.app.Application;

import com.pyrus.pyrusservicedesk.PyrusServiceDesk;

/**
 * SDK is initialized from {@link SampleActivity} so two different appId / credential sets
 * can be exercised. Do not call {@link PyrusServiceDesk#init} here if you need that flexibility.
 */
public class SampleApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PyrusServiceDesk.onAuthorizationFailed(null);
    }
}
