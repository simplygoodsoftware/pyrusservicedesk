package com.pyrus.servicedesksample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.pyrus.pyrusservicedesk.PyrusServiceDesk;
import com.pyrus.pyrusservicedesk.ServiceDeskConfiguration;
import com.pyrus.pyrusservicedesk.sdk.updates.NewReplySubscriber;

import java.util.Collections;
import java.util.List;

public class SampleActivity extends Activity implements NewReplySubscriber {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        findViewById(R.id.support).setOnClickListener(
                view -> PyrusServiceDesk.start(
                        this,
                        new ServiceDeskConfiguration.Builder()
                                .setUserName("Ivan Ivanov")
                                .setStartMessage("hello it is test of comment 2")
                                .setThemeColor(Color.parseColor("#FF8300"))
                                .setChatTitle("Sample Support")
                                .setWelcomeMessage("How can I help you?")
                                .setAvatarForSupport(com.pyrus.pyrusservicedesk.R.drawable.psd_download_file)
                                .setChatMenuDelegate(new ChatMenuDelegate())
                                .setTrustedUrls(Collections.singletonList("pyrus.com"))
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
    protected void onStart() {
        super.onStart();
        PyrusServiceDesk.subscribeToReplies(this);
    }

    @Override
    protected void onStop() {
        PyrusServiceDesk.unsubscribeFromReplies(this);
        super.onStop();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onNewReply(
            boolean hasUnreadComments,
            @Nullable String lastCommentText,
            int lastCommentAttachmentsCount,
            @Nullable List<String> lastCommentAttachments,
            long utcTime
    ) {

        String text;
        if (lastCommentText != null)
            text = lastCommentText;
        else
            text = "no text";

        StringBuilder attachmentNames = new StringBuilder();
        if (lastCommentAttachments != null)
            for (String attachmentName: lastCommentAttachments)
                attachmentNames.append(attachmentName);

        ((TextView) findViewById(R.id.info)).setText(
                "Has unread tickets: " + hasUnreadComments + "\n" +
                "Last comment text: " + text + "\n" +
                "Attachments count: " + lastCommentAttachmentsCount + "\n" +
                "AttachmentNames: " + attachmentNames
        );
    }
}
