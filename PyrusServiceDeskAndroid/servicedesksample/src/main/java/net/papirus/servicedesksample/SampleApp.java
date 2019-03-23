package net.papirus.servicedesksample;

import android.app.Application;
import net.papirus.pyrusservicedesk.PyrusServiceDesk;

public class SampleApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PyrusServiceDesk.init(
                this,
                "CdK+zJZfbm3IrC/LEAiV6auwdi9BKRNf/1ZL4FmombJZNMKEqPoLAT7ft22dmhZfTXwzOZPur0fcx+JRDeo3+o60ylppDh+mll002Q169kizb0uTefnY7yayyp0is6ttWJjRcg=="
        );
    }
}
