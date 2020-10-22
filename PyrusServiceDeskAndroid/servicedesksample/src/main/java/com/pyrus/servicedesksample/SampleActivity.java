package com.pyrus.servicedesksample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import com.pyrus.pyrusservicedesk.PyrusServiceDesk;
import com.pyrus.pyrusservicedesk.ServiceDeskConfiguration;
import com.pyrus.pyrusservicedesk.sdk.updates.NewReplySubscriber;

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
                                .setChatMenuDelegate(new ChatMenuDelegate())
                                .build())
        );

        PyrusServiceDesk.onAuthorizationFailed(
                () -> {
                    AlertDialog dialog = new AlertDialog
                            .Builder(this)
                            .create();

                    dialog.setTitle("Authorization Error.");
                    dialog.setMessage("Failed to authorize with the provided credentials.");
                    dialog.setButton(
                            DialogInterface.BUTTON_POSITIVE,
                            "OK",
                            (dialog1, which) -> {
                                PyrusServiceDesk.init(
                                        getApplication(),
                                        "my_app_id"
                                );
                                dialog1.cancel();
                            }
                    );

                    dialog.show();
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PyrusServiceDesk.unsubscribeFromReplies(this);
    }

    @Override
    public void onNewReply(boolean hasUnreadComments) {
        ((TextView) findViewById(R.id.unread)).setText(
                hasUnreadComments
                        ? "Has unread tickets"
                        : null
        );
    }
}
