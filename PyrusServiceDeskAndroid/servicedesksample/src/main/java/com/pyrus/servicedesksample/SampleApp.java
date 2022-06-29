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
                "tbj8JqAyTXfDQ7eT2oE4j6M12nChJNtDD0rr~5P0Hx2uvAUngTzsea0fZq0IzE8V82Z8VskSP95G~LOHJrwEQbPS4o1vUf5K8DVq7w0IVxfSyQqwPy0rC-9QrvTGORZPLHKWIg=="
        );

        PyrusServiceDesk.setPushToken(
                "my_push_token",
                exception -> Log.w("SAMPLE_APP", exception));

        PyrusServiceDesk.onAuthorizationFailed(null);
    }
}
