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
                "Hsdzu8HtZfQtQDv6avpE~Fh8asQPZ32ScwI4k0MSTaJg7W~TcPydLXQMSvQ-dQas30AwKWqBnfkyUzV9fbNSvdCTgRvEc8lLG8JjAbzT66Yc9DfjHaw7LXUfIqJ9ezSIYASlgw==",
                "394513",
                "3iToVqa6/jxs7+CgwPkpyLKpRKfbH6P7e9kq5ENWOKF3LmXdxVjpKHwxiUKD9XfOCwosTrvSmUDMBw8qGq2ICA==",
                true
        );

        PyrusServiceDesk.setPushToken(
                "my_push_token",
                exception -> Log.w("SAMPLE_APP", exception));

        PyrusServiceDesk.onAuthorizationFailed(null);
    }
}
