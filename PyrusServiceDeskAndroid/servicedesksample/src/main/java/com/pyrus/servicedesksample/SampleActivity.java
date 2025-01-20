package com.pyrus.servicedesksample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.pyrus.pyrusservicedesk.PyrusServiceDesk;
import com.pyrus.pyrusservicedesk.ServiceDeskConfiguration;
import com.pyrus.pyrusservicedesk.User;
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.MultichatButtons;
import com.pyrus.pyrusservicedesk.sdk.updates.NewReplySubscriber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SampleActivity extends Activity implements NewReplySubscriber {

    private User user1 = new User("255371017", "xZlr1Zf0pZZE43NfjXfY10OvEKwkKLRCO~PYF7SjID-Tp-7sK5EAuWqgOfrCQNOdDUHrZhHlBaqcdzj2ULgf9e~ciFudXo9ff1Y9cx0oXaTGziZKANoCLbWceaF-5g1VAQpfcg==", "Ресторан 1");
    private User user2 = new User("251380375", "n4Mxu60kICP-XtZkGm2zCRlDtRRBi76h1w7FMx~f2F~z3d~Ayz7~Z7Gfxg7q2dI~sNVS965oM44Buy8uX2ngWib4BIIaf~6uIT6KaRzyGn2N6O2zdj-lufplexg1TvYLTviMSw==", "Много Лосося ДК Москва, Большая Филёвская улица, 3");
    private User user3 = new User("251374579", "n4Mxu60kICP-XtZkGm2zCRlDtRRBi76h1w7FMx~f2F~z3d~Ayz7~Z7Gfxg7q2dI~sNVS965oM44Buy8uX2ngWib4BIIaf~6uIT6KaRzyGn2N6O2zdj-lufplexg1TvYLTviMSw==", "Старик Хинкалыч - Кострома Коллаж");



    private final ArrayList<User> users = new ArrayList<>();

    int count = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        findViewById(R.id.support).setOnClickListener(view -> {
            count++;
//            if (count == 1)
//                users.add(user1);
//            else if (count == 2)
//                users.add(user2);
//            else if (count == 3)
//                users.add(user3);
            users.add(user1);
            users.add(user2);
            users.add(user3);
            PyrusServiceDesk.initAsMultichat(getApplication(), users, "10");

            Map<String, String> map = new HashMap<>();
            map.put("test_text", "test some text");
            map.put("test_number", "999");
            map.put("test_money", "555");
            map.put("test_phone", "79778888888");
            map.put("test_email", "sample@email.com");
            PyrusServiceDesk.setFieldsData(map);

            Intent intent = new Intent(this, SampleActivity.class);
            intent.putExtra("FRAGMENT_KEY", "FRAGMENT_QR_KEY");
            ServiceDeskConfiguration builder =  new ServiceDeskConfiguration.Builder()
                    .setUserName("ssss")
                    .setWelcomeMessage("How can I help you?")
                    .setAvatarForSupport(R.drawable.psd_download_file)
                    .setChatMenuDelegate(new ChatMenuDelegate())
                    .setTrustedUrls(Collections.singletonList("pyrus.com"))
                    .setMultichatButtons(new MultichatButtons(R.drawable.ic_qr, intent, intent))
                    .build();

            PyrusServiceDesk.start(this, builder);
        });



        PyrusServiceDesk.onAuthorizationFailed(() -> {
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
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
//        PyrusServiceDesk.subscribeToReplies(this);
    }

    @Override
    protected void onStop() {
//        PyrusServiceDesk.unsubscribeFromReplies(this);
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
