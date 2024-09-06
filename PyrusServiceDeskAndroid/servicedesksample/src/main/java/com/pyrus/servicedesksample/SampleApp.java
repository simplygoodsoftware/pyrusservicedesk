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
            "GJv5lsZnNQM8jDwppzj8Ciuwm5MiuQsF41nXoUkNut2qETsjtJz0F271-~M6WFfukoW6YJeiMZAjKzAiehLIFkklDpQ1Spap0Yd9csgIdVJTAdCL0ZF~bPl6qLvH0UfnFY-Z8A==",
            null,
            true
        );

//        PyrusServiceDesk.setPushToken(
//                "my_push_token",
//                exception -> Log.w("SAMPLE_APP", exception));

        PyrusServiceDesk.onAuthorizationFailed(null);
    }
}
