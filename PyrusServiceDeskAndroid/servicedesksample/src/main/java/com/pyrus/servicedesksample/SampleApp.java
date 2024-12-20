package com.pyrus.servicedesksample;

import android.app.Application;

import com.pyrus.pyrusservicedesk.PyrusServiceDesk;
import com.pyrus.pyrusservicedesk.sdk.data.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SampleApp extends Application {

    private User user1 = new User("255371017", "xZlr1Zf0pZZE43NfjXfY10OvEKwkKLRCO~PYF7SjID-Tp-7sK5EAuWqgOfrCQNOdDUHrZhHlBaqcdzj2ULgf9e~ciFudXo9ff1Y9cx0oXaTGziZKANoCLbWceaF-5g1VAQpfcg==", "Ресторан 1");
    private User user2 = new User("251380375", "n4Mxu60kICP-XtZkGm2zCRlDtRRBi76h1w7FMx~f2F~z3d~Ayz7~Z7Gfxg7q2dI~sNVS965oM44Buy8uX2ngWib4BIIaf~6uIT6KaRzyGn2N6O2zdj-lufplexg1TvYLTviMSw==", "Много Лосося ДК Москва, Большая Филёвская улица, 3");
    private User user3 = new User("251374579", "n4Mxu60kICP-XtZkGm2zCRlDtRRBi76h1w7FMx~f2F~z3d~Ayz7~Z7Gfxg7q2dI~sNVS965oM44Buy8uX2ngWib4BIIaf~6uIT6KaRzyGn2N6O2zdj-lufplexg1TvYLTviMSw==", "Старик Хинкалыч - Кострома Коллаж");



    private final ArrayList<User> users = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();

        users.add(user1);
        PyrusServiceDesk.init(
                this, users, "10", true);

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
