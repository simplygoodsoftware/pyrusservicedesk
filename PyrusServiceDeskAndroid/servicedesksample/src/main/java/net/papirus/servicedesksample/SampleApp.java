package net.papirus.servicedesksample;

import android.app.Application;
import net.papirus.pyrusservicedesk.PyrusServiceDesk;

public class SampleApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PyrusServiceDesk.init(
                this,
                "b7206b43-6859-4a20-837d-637a68e92d94",
                12345,
                "Android Sample",
                true);
    }
}
