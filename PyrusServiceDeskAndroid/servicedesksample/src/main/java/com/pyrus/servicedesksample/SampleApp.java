package com.pyrus.servicedesksample;

import android.app.Application;

import com.pyrus.pyrusservicedesk.PyrusServiceDesk;

import java.util.HashMap;
import java.util.Map;

public class SampleApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Map<String, String> map = new HashMap<>();

        map.put("test_text", "test some text");
        map.put("test_number", "999");
        map.put("test_money", "555");
        map.put("test_phone", "79778888888");
        map.put("test_email", "sample@email.com");
        PyrusServiceDesk.setFieldsData(map);

//        dodo sample
//        "GJv5lsZnNQM8jDwppzj8Ciuwm5MiuQsF41nXoUkNut2qETsjtJz0F271-~M6WFfukoW6YJeiMZAjKzAiehLIFkklDpQ1Spap0Yd9csgIdVJTAdCL0ZF~bPl6qLvH0UfnFY-Z8A=="
        PyrusServiceDesk.init(
            this,
            "0HUi7grFuWVFHqWtL3f5YD-4PYJXiOEoLfCDb2yhTthkHBpedNbNU4O01YD2OnsSpvbMiXmweUF8akomZZIW1Ilb-W9mOPuK70L4lCI1mK0dJqXYUp0l-MJlsUv9tr8dSmKCSw==",
            "dev.pyrus.com",
            true,
            null
        );

        PyrusServiceDesk.onAuthorizationFailed(null);
    }
}
