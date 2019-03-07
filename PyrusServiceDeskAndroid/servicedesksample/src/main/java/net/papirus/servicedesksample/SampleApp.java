package net.papirus.servicedesksample;

import android.app.Application;
import net.papirus.pyrusservicedesk.PyrusServiceDesk;

public class SampleApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PyrusServiceDesk.init(this, "ClientId", "Sample");
    }
}
