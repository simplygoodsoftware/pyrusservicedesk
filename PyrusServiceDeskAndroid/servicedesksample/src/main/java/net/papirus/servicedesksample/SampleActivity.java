package net.papirus.servicedesksample;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.widget.TextView;
import net.papirus.pyrusservicedesk.PyrusServiceDesk;
import net.papirus.pyrusservicedesk.ServiceDeskConfigure;
import net.papirus.pyrusservicedesk.UnreadCounterChangedSubscriber;

public class SampleActivity extends Activity implements UnreadCounterChangedSubscriber {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        findViewById(R.id.support).setOnClickListener(
                view -> PyrusServiceDesk.start(
                        this,
                        new ServiceDeskConfigure.Builder()
                                .setUserName("Ivan Ivanov")
                                .setThemeColor(Color.parseColor("#FF8300"))
                                .setChatTitle("AMAZING")
                                .setWelcomeMessage("Why so serious?")
                                .setAvatarForSupport(drawable())
                                .build())
        );
    }

    private BitmapDrawable drawable() {
        Bitmap bmp = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawRGB(0, 255, 0);
        return new BitmapDrawable(getResources(), bmp);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PyrusServiceDesk.unsubscribeFromUnreadCounterChanged(this);
    }

    @Override
    public void onUnreadCounterChanged(int unreadCounter) {
        ((TextView)findViewById(R.id.unread)).setText("Unread: " + unreadCounter);
    }
}
