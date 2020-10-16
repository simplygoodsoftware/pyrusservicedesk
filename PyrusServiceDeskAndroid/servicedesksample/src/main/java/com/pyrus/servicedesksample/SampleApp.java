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
                "24Ed4oIBtoeHnqDHNU5O7zLeOILVziiMP2pkghQZAL1SXHztt0w8HSv5u9LNGitjNR~pU3JOLPGt3jq-1jwwDUjJzgqlvV3TpyDyD8Bq72clDyhWyOrW8lr9lf4yfrwt0tb80w=="
        );

        PyrusServiceDesk.setPushToken(
                "my_push_token",
                exception -> {
                    Log.w("SAMPLE_APP", exception);
                });

    }
}
