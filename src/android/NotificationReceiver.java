package org.apache.cordova.firebase;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.RemoteInput;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import java.util.Date;

import org.apache.cordova.firebase.actions.InlineReplyAction;
import org.apache.cordova.firebase.actions.MarkAsReadAction;
import org.apache.cordova.firebase.actions.RejectCallAction;
import org.apache.cordova.firebase.actions.SnoozeAction;
import org.apache.cordova.firebase.actions.MailOptionsAction;
import org.apache.cordova.firebase.actions.MailReplyAction;

import org.apache.cordova.firebase.models.MailInfoItem;

import org.apache.cordova.firebase.utils.NotificationUtils;

import org.apache.cordova.firebase.notification.NotificationCreator;
import org.apache.cordova.firebase.OnNotificationOpenReceiver;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "Firebase.NotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().contains(NotificationCreator.NOTIFICATION_REPLY)) {
            String[] actionParts = intent.getAction().split("@@");
            int notificationId = Integer.parseInt(actionParts[1]);
            String target = actionParts[2];
            CharSequence message = getReplyMessage(intent);

            Log.i(TAG, "NotificationReceiver onReceive NotificationReply, notificationId: " + notificationId + ", target: " + target + ", message: " + message);

            if (message != null && message.length() > 0) {
                Thread thread = new Thread(new InlineReplyAction(message.toString(), target, notificationId, context));
                thread.start();
            }
        } else if (intent.getAction().contains(NotificationCreator.MARK_AS_READ_REPLY)) {
            String[] actionParts = intent.getAction().split("@@");
            int notificationId = Integer.parseInt(actionParts[1]);
            String target = actionParts[2];

            Log.i(TAG, "NotificationReceiver onReceive MarkAsReadReply, notificationId: " + notificationId + ", target: " + target);

            Thread thread = new Thread(new MarkAsReadAction(target, notificationId, context));
            thread.start();
        } else if (intent.getAction().contains(NotificationCreator.SNOOZE_REPLY)) {
            String[] actionParts = intent.getAction().split("@@");
            int notificationId = Integer.parseInt(actionParts[1]);
            String taskId = actionParts[2];

            Log.i(TAG, "NotificationReceiver onReceive Snooze, notificationId: " + notificationId + ", taskId: " + taskId);

            Date remindOn = new Date(System.currentTimeMillis() + 3600 * 1000);
            Thread thread = new Thread(new SnoozeAction(taskId, notificationId, remindOn, context));
            thread.start();
        } else if (intent.getAction().contains(NotificationCreator.MAIL_MARK_AS_READ)) {
            String[] actionParts = intent.getAction().split("@@");
            int notificationId = Integer.parseInt(actionParts[1]);
            Integer msgId = Integer.parseInt(actionParts[2]);

            Log.i(TAG, "NotificationReceiver onReceive MarkAsRead, notificationId: " + notificationId + ", msgId: " + msgId);

            Thread thread = new Thread(new MailOptionsAction(context, notificationId, "read", msgId));
            thread.start();
        } else if (intent.getAction().contains(NotificationCreator.MAIL_DELETE)) {
            String[] actionParts = intent.getAction().split("@@");
            int notificationId = Integer.parseInt(actionParts[1]);
            Integer msgId = Integer.parseInt(actionParts[2]);

            Log.i(TAG, "NotificationReceiver onReceive Delete, notificationId: " + notificationId + ", msgId: " + msgId);

            Thread thread = new Thread(new MailOptionsAction(context, notificationId, "trash", msgId));
            thread.start();
        } else if (intent.getAction().contains(NotificationCreator.MAIL_NOTIFICATION_REPLY)) {
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if(remoteInput == null) {
                Log.w(TAG, "NotificationReceiver onReceive Mail Reply, reply Bundle is NULL");
                return;
            }

            String[] actionParts = intent.getAction().split("@@");
            int notificationId = Integer.parseInt(actionParts[1]);
            String msgId = actionParts[2];
            String subject = actionParts[3];
            String fromAddress = actionParts[4];
            String fromDisplay = actionParts[5];
            String replyText = remoteInput.getCharSequence("Reply").toString();

            Log.i(TAG, "NotificationReceiver onReceive Reply, notificationId: " + notificationId + ", msgId: " + msgId);

            MailInfoItem receiver = new MailInfoItem();
            receiver.type = "t";
            receiver.address = fromAddress;
            receiver.displayName = fromDisplay;

            Thread thread = new Thread(new MailReplyAction(context, notificationId, msgId, subject, replyText, receiver));
            thread.start();
        } else if (intent.getAction().contains(NotificationCreator.TALK_CALL_DECLINE)) {
            String[] actionParts = intent.getAction().split("@@");
            String callId = actionParts[1];
            String callType = actionParts[2];
            String callReceiver = actionParts[3];
            boolean isGroupCall = Boolean.parseBoolean(actionParts[4]);

            Log.i(TAG, "NotificationReceiver onReceive Call REJECT, callId: " + callId);

            LocalBroadcastManager.getInstance(context.getApplicationContext())
                .sendBroadcast(new Intent(NotificationCreator.TALK_CALL_DECLINE).putExtra("extra_call_id", callId));
            
            Thread thread = new Thread(new RejectCallAction(context, callId, callType, callReceiver, isGroupCall));
            thread.start();
        } else if (intent.getAction().contains(NotificationCreator.TALK_CALL_ACCEPT)) {
            String[] actionParts = intent.getAction().split("@@");
            String callId = actionParts[1];
            String callType = actionParts[2];
            int callNotificationId = NotificationUtils.generateCallNotificationId(callId);

            Log.i(TAG, "NotificationReceiver onReceive Call ACCEPT, callId: " + callId);

            Intent launchIntent = new Intent(context.getApplicationContext(), OnNotificationOpenReceiver.class);
            Bundle bundle = new Bundle();
            bundle.putString("vncPeerJid", callId);
            bundle.putString("vncEventType", callType);
            bundle.putInt("id", callNotificationId);
            launchIntent.putExtras(bundle);

            context.sendBroadcast(launchIntent);

            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(callNotificationId);
            LocalBroadcastManager.getInstance(context.getApplicationContext())
                .sendBroadcast(new Intent(NotificationCreator.TALK_CALL_ACCEPT).putExtra("extra_call_id", callId));
        }
    }

    private CharSequence getReplyMessage(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence("Reply");
        }
        return null;
    }
}
