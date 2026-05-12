package com.pyrus.servicedesksample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.pyrus.pyrusservicedesk.PyrusServiceDesk;
import com.pyrus.pyrusservicedesk.ServiceDeskConfiguration;
import com.pyrus.pyrusservicedesk.sdk.updates.NewReplySubscriber;
import com.pyrus.pyrusservicedesk.sdk.updates.OnStopCallback;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SampleActivity extends Activity {

    private TextView statusView;
    private TextView replyInfoA;
    private TextView replyInfoB;
    private CheckBox aLogging;
    private CheckBox bLogging;
    private NewReplySubscriber subscriberA;
    private NewReplySubscriber subscriberB;

    private String lastInitLabel = "(none)";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        statusView = findViewById(R.id.status);
        replyInfoA = findViewById(R.id.a_reply_info);
        replyInfoB = findViewById(R.id.b_reply_info);


        aLogging = findViewById(R.id.a_logging);
        bLogging = findViewById(R.id.b_logging);


        replyInfoA.setText(R.string.sample_reply_placeholder_a);
        replyInfoB.setText(R.string.sample_reply_placeholder_b);

        subscriberA = new LabeledReplySubscriber("A", () -> replyInfoA);
        subscriberB = new LabeledReplySubscriber("B", () -> replyInfoB);

        findViewById(R.id.a_init).setOnClickListener(v ->
            initProfile("A",
                "94y1TqXCkoYnlxomas~Cfq7mDUYR9o0HYhfZM5RI4SCXBLP9ghJPEV0qfXzbq00lKkrkZe3rzHACxbAmsbU8LgTiCu8Gg5bUJGR6sW3YBJhNtf8xsFBOtuqV0Fz-Ahu2OSgCng==",
                null,
                null,
                aLogging)
        );
        findViewById(R.id.a_start).setOnClickListener(v ->
            startDesk(
                "A",
                Color.parseColor("#FF8300"),
                "Support (IVI)",
                "Ivan Ivanov A",
                "Welcome A",
                "")
        );
        findViewById(R.id.a_subscribe).setOnClickListener(v -> subscribe(subscriberA));
        findViewById(R.id.a_unsubscribe).setOnClickListener(v -> unsubscribe(subscriberA));

        findViewById(R.id.b_init).setOnClickListener(v ->
            initProfile("B",
                "n4Mxu60kICP-XtZkGm2zCRlDtRRBi76h1w7FMx~f2F~z3d~Ayz7~Z7Gfxg7q2dI~sNVS965oM44Buy8uX2ngWib4BIIaf~6uIT6KaRzyGn2N6O2zdj-lufplexg1TvYLTviMSw==",
                "userId",
                "securityKey",
                aLogging)
        );
        findViewById(R.id.b_start).setOnClickListener(v ->
            startDesk(
                "B",
                Color.parseColor("#1565C0"),
                "Support",
                "Ivan Ivanov B",
                "Welcome B",
                "Привет")
        );
        findViewById(R.id.b_subscribe).setOnClickListener(v -> subscribe(subscriberB));
        findViewById(R.id.b_unsubscribe).setOnClickListener(v -> unsubscribe(subscriberB));

        PyrusServiceDesk.onAuthorizationFailed(
            () -> {
                AlertDialog dialog = new AlertDialog.Builder(this).create();
                dialog.setTitle("Authorization error");
                dialog.setMessage("Check credentials, then Init the correct profile again.");
                dialog.setButton(
                    DialogInterface.BUTTON_POSITIVE,
                    "OK",
                    (d, which) -> d.dismiss()
                );
                dialog.show();
            }
        );
    }

    @Override
    protected void onDestroy() {
        PyrusServiceDesk.unsubscribeFromReplies(subscriberA);
        PyrusServiceDesk.unsubscribeFromReplies(subscriberB);
        super.onDestroy();
    }

    private void initProfile(
        String label,
        String appId,
        String userId,
        String security,
        CheckBox loggingCb
    ) {

        boolean logging = loggingCb.isChecked();
        if (userId == null || security == null) {
            PyrusServiceDesk.init(
                getApplication(),
                appId,
                null,
                logging
            );
        }
        else {
            PyrusServiceDesk.init(
                getApplication(),
                appId,
                userId,
                security,
                null,
                logging
            );
        }

        Map<String, String> fields = new HashMap<>();
        fields.put("test_text", "sample " + label);
        fields.put("test_number", label.equals("A") ? "111" : "222");
        PyrusServiceDesk.setFieldsData(fields);
        PyrusServiceDesk.setPushToken(
            "my_push_token",
            exception -> Log.w("SAMPLE_APP", exception));

        lastInitLabel = label;
        statusView.setText(getString(R.string.sample_status_last_init, label, appId, userId));
    }

    private void startDesk(
        String label,
        int themeColor,
        String chatTitle,
        String userName,
        String welcome,
        String sendComment
    ) {
        ServiceDeskConfiguration config = new ServiceDeskConfiguration.Builder()
            .setUserName(userName)
            .setThemeColor(themeColor)
            .setChatTitle(chatTitle)
            .setWelcomeMessage(welcome)
            .setAvatarForSupport(com.pyrus.pyrusservicedesk.R.drawable.psd_download_file)
            .setTrustedUrls(Collections.singletonList("pyrus.com"))
            .build();
        OnStopCallback onStopCallback = () -> Toast.makeText(this, "On stopCallback", Toast.LENGTH_LONG).show();
        PyrusServiceDesk.start(this, config, onStopCallback, sendComment);
        statusView.setText(getString(R.string.sample_status_started, label, lastInitLabel));
    }

    private void subscribe(NewReplySubscriber subscriber) {
        PyrusServiceDesk.subscribeToReplies(subscriber);
        Toast.makeText(this, "Subscribed", Toast.LENGTH_SHORT).show();
    }

    private void unsubscribe(NewReplySubscriber subscriber) {
        PyrusServiceDesk.unsubscribeFromReplies(subscriber);
        Toast.makeText(this, "Unsubscribed", Toast.LENGTH_SHORT).show();
    }

    private class LabeledReplySubscriber implements NewReplySubscriber {

        private final String label;
        private final TextViewSupplier targetSupplier;

        LabeledReplySubscriber(String label, TextViewSupplier targetSupplier) {
            this.label = label;
            this.targetSupplier = targetSupplier;
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
            String text = lastCommentText != null ? lastCommentText : "no text";
            StringBuilder names = new StringBuilder();
            if (lastCommentAttachments != null) {
                for (String n : lastCommentAttachments) {
                    names.append(n).append(' ');
                }
            }
            String body = "[" + label + "] unread=" + hasUnreadComments
                + "\ntext: " + text
                + "\nattachments: " + lastCommentAttachmentsCount
                + " " + names
                + "\nutc: " + utcTime;

            runOnUiThread(() -> targetSupplier.get().setText(body));
        }
    }

    private interface TextViewSupplier {
        TextView get();
    }
}
