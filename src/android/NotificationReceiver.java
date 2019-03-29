package org.apache.cordova.firebase;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;
import android.util.Log;
import java.util.Date;

import org.apache.cordova.firebase.actions.InlineReplyAction;
import org.apache.cordova.firebase.actions.MarkAsReadAction;
import org.apache.cordova.firebase.actions.SnoozeAction;

import org.apache.cordova.firebase.notification.NotificationCreator;

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

            Log.i(TAG, "NotificationReceiver onReceive MarkAsReadReply, notificationId: " + notificationId + ", taskId: " + taskId);

            Date remindOn = new Date(System.currentTimeMillis() - 3600 * 1000);
            Thread thread = new Thread(new SnoozeAction(taskId, notificationId, remindOn, context));
            thread.start();
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
