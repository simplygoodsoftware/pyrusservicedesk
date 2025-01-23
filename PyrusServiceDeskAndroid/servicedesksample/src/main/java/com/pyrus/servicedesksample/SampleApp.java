package com.pyrus.servicedesksample;

import android.app.Application;

import com.pyrus.pyrusservicedesk.PyrusServiceDesk;
import com.pyrus.pyrusservicedesk.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SampleApp extends Application {

    private final ArrayList<User> users = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();

//        users.add(PyrusServiceDesk.user1());
//        users.add(PyrusServiceDesk.user2());
//        users.add(PyrusServiceDesk.user3());
        users.add(PyrusServiceDesk.user4());
        PyrusServiceDesk.initAsMultichat(this, users, "10");

        Map<String, String> map = new HashMap<>();
        map.put("test_text", "test some text");
        map.put("test_number", "999");
        map.put("test_money", "555");
        map.put("test_phone", "79778888888");
        map.put("test_email", "sample@email.com");
        PyrusServiceDesk.setFieldsData(map);

        // TODO
//        PyrusServiceDesk.setPushToken(
//            "my_push_token",
//            exception -> Log.w("SAMPLE_APP", exception)
//        );

        PyrusServiceDesk.onAuthorizationFailed(null);
    }
}
