package net.papirus.servicedesksample;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import net.papirus.pyrusservicedesk.PyrusServiceDesk;
import net.papirus.pyrusservicedesk.ServiceDeskConfiguration;
import net.papirus.pyrusservicedesk.sdk.updates.NewReplySubscriber;

public class SampleActivity extends Activity implements NewReplySubscriber {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        PyrusServiceDesk.subscribeToReplies(this);
        findViewById(R.id.support).setOnClickListener(
                view -> PyrusServiceDesk.start(
                        this,
                        new ServiceDeskConfiguration.Builder()
                                .setUserName("Ivan Ivanov")
                                .setThemeColor(Color.parseColor("#FF8300"))
                                .setChatTitle("Sample Support")
                                .setWelcomeMessage("How can I help you?")
                                .setAvatarForSupport(R.drawable.psd_download_file)
                                .build())
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PyrusServiceDesk.unsubscribeFromReplies(this);
    }

    @Override
    public void onNewReply() {
        ((TextView)findViewById(R.id.unread)).setText("Has unread tickets");
    }
}
