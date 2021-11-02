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
            "cdT-ZLBgrDP9bot0Cy4Cpd1~XLljnKl28gZwxSYxBVG7Wlr6EyLpayocdEyuPEQ~W1HT7i3-5M5p-E9kVMkGZwVDbL3mAUclExos4cSP4cfeUa8wQsAn4mPy4LzoF9IrpMYKhA==",
            "demo.kiloplan.ru",
            true
        );

        PyrusServiceDesk.setPushToken(
                "my_push_token",
                exception -> Log.w("SAMPLE_APP", exception));

        PyrusServiceDesk.onAuthorizationFailed(null);
    }
}
