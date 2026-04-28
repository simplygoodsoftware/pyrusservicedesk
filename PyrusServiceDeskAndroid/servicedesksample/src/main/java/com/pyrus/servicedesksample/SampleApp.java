package com.pyrus.servicedesksample;

import android.app.Application;
import android.util.Log;

import com.pyrus.pyrusservicedesk.PyrusServiceDesk;

import java.util.HashMap;
import java.util.Map;

public class SampleApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PyrusServiceDesk.init(
            this,
            "94y1TqXCkoYnlxomas~Cfq7mDUYR9o0HYhfZM5RI4SCXBLP9ghJPEV0qfXzbq00lKkrkZe3rzHACxbAmsbU8LgTiCu8Gg5bUJGR6sW3YBJhNtf8xsFBOtuqV0Fz-Ahu2OSgCng==",
            null,
            true
        );

        Map<String, String> map = new HashMap<>();
        map.put("test_text", "test some text");
        map.put("test_number", "999");
        map.put("test_money", "555");
        map.put("test_phone", "79778888888");
        map.put("test_email", "sample@email.com");
        PyrusServiceDesk.setFieldsData(map);

        PyrusServiceDesk.setPushToken(
                "my_push_token",
                exception -> Log.w("SAMPLE_APP", exception));

        PyrusServiceDesk.onAuthorizationFailed(null);
    }
}
