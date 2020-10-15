package com.pyrus.servicedesksample;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import com.pyrus.pyrusservicedesk.PyrusServiceDesk;
import com.pyrus.pyrusservicedesk.ServiceDeskConfiguration;
import com.pyrus.pyrusservicedesk.sdk.updates.NewReplySubscriber;
import com.pyrus.pyrusservicedesk.PyrusServiceDesk;
import com.pyrus.pyrusservicedesk.ServiceDeskConfiguration;
import com.pyrus.pyrusservicedesk.sdk.updates.NewReplySubscriber;
import com.pyrus.servicedesksample.R;

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
    public void onNewReply(boolean hasUnreadComments) {
        ((TextView)findViewById(R.id.unread)).setText("Has unread tickets");
    }
}
