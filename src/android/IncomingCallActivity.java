package org.apache.cordova.firebase;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class IncomingCallActivity extends AppCompatActivity {

    private static final String EXTRA_CALL_ID = "extra_call_id";
    private static final String EXTRA_CALL_TYPE = "extra_call_type";
    private static final String EXTRA_CALL_RECEIVER = "extra_call_receiver";
    private static final String EXTRA_CALL_TITLE = "extra_call_title";
    private static final String EXTRA_CALL_SUBTITLE = "extra_call_subtitle";
    private static final String EXTRA_IS_GROUP_CALL = "extra_is_group_call";

    private static final String TALK_CALL_DECLINE = "TalkCallDecline";
    private static final String TALK_CALL_ACCEPT = "TalkCallAccept";

    private BroadcastReceiver callStateReceiver;
    private LocalBroadcastManager localBroadcastManager;

    private String callId;
    private String callType;
    private String callReceiver;
    private String callTitle;
    private String callSubTitle;
    private boolean isGroupCall;

    public static Intent createStartIntent(Context context, String callId, String callType,
                                           String callReceiver, String title, String subTitle, boolean isGroupCall){
        Intent intent = new Intent(context, IncomingCallActivity.class);
        intent.putExtra(EXTRA_CALL_ID, callId);
        intent.putExtra(EXTRA_CALL_TYPE, callType);
        intent.putExtra(EXTRA_CALL_RECEIVER, callReceiver);
        intent.putExtra(EXTRA_CALL_TITLE, title);
        intent.putExtra(EXTRA_CALL_SUBTITLE, subTitle);
        intent.putExtra(EXTRA_IS_GROUP_CALL, isGroupCall);

        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(getResources().getIdentifier("activity_incoming_call", "layout", getPackageName()));

        processIncomingData(getIntent());
        initUi();
        initCallStateReceiver();
    }

    private void initCallStateReceiver() {
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        callStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || TextUtils.isEmpty(intent.getAction())) return;

                if (TALK_CALL_DECLINE.equals(intent.getAction()) || TALK_CALL_ACCEPT.equals(intent.getAction())) {
                    finish();
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TALK_CALL_DECLINE);
        intentFilter.addAction(TALK_CALL_ACCEPT);
        localBroadcastManager.registerReceiver(callStateReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        localBroadcastManager.unregisterReceiver(callStateReceiver);
    }

    private void processIncomingData(Intent intent) {
        callId = intent.getStringExtra(EXTRA_CALL_ID);
        callType = intent.getStringExtra(EXTRA_CALL_TYPE);
        callReceiver = intent.getStringExtra(EXTRA_CALL_RECEIVER);
        callTitle = intent.getStringExtra(EXTRA_CALL_TITLE);
        callSubTitle = intent.getStringExtra(EXTRA_CALL_SUBTITLE);
        isGroupCall = intent.getBooleanExtra(EXTRA_IS_GROUP_CALL, false);
    }

    private void initUi() {
        TextView callTitleTxt = findViewById(getResources().getIdentifier("user_name_txt", "id", getPackageName()));
        callTitleTxt.setText(callTitle);

        TextView callSubTitleTxt = findViewById(getResources().getIdentifier("call_type_txt", "id", getPackageName()));
        callSubTitleTxt.setText(callSubTitle);
    }

    public void onEndCall(View view) {
        String callDeclineActionName = TALK_CALL_DECLINE
                + "@@" + callId
                + "@@" + callType
                + "@@" + callReceiver
                + "@@" + isGroupCall;

        Intent endCallIntent = new Intent(this, NotificationReceiver.class);
        endCallIntent.setAction(callDeclineActionName);

        getApplicationContext().sendBroadcast(endCallIntent);
        finish();
    }

    public void onStartCall(View view) {
        String callAcceptActionName = TALK_CALL_ACCEPT
                + "@@" + callId
                + "@@" + callType;

        Intent endCallIntent = new Intent(this, NotificationReceiver.class);
        endCallIntent.setAction(callAcceptActionName);

        getApplicationContext().sendBroadcast(endCallIntent);
        finish();
    }
}