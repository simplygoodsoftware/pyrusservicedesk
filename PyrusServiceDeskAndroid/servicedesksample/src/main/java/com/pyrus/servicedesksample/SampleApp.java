package com.pyrus.servicedesksample;

import android.app.Application;
import android.util.Log;

import com.pyrus.pyrusservicedesk.PyrusServiceDesk;

public class SampleApp extends Application {

    public static final String APP_ID = "Hsdzu8HtZfQtQDv6avpE~Fh8asQPZ32ScwI4k0MSTaJg7W~TcPydLXQMSvQ-dQas30AwKWqBnfkyUzV9fbNSvdCTgRvEc8lLG8JjAbzT66Yc9DfjHaw7LXUfIqJ9ezSIYASlgw==";

    @Override
    public void onCreate() {
        super.onCreate();
        PyrusServiceDesk.init(
                this,
                APP_ID,
                "394513",
                "vVIE+SQdDreBsS9GIewTfT+fIxuCAoppuIfjmT5DbpA/t42sdTwetanQr9z6cmXujDZen44A5Pi+c/Zryv26/g==",
                true
        );

        PyrusServiceDesk.setPushToken(
                "my_push_token",
                exception -> Log.w("SAMPLE_APP", exception));

        PyrusServiceDesk.onAuthorizationFailed(null);
    }
}
