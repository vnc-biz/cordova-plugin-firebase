package org.apache.cordova.firebase.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.apache.cordova.firebase.utils.NotificationUtils;

import java.util.ArrayList;
import java.util.List;

public class NotificationManager {

    private static final String TAG = "NotificationDisplay";

    private static final String PREVIOUS_MESSAGES = "previousMessages";
    private static final String NOTIFY_ID_FOR_UPDATING = "notifIdForUpdating";
    private static final String MESSAGE_TARGET = "messageTarget";


    public static void displayNotification(Context activityOrServiceContext, Context appContext,
                                           String id, String target, String name, String groupName,
                                           String message, String eventType, String nsound,
                                           boolean showNotification, String sound, String lights) {
        Log.i(TAG, "displayNotification: Target: " + target);
        Log.i(TAG, "displayNotification: username: " + name);
        Log.i(TAG, "displayNotification: groupName: " + groupName);
        Log.i(TAG, "displayNotification: message: " + message);
        Log.i(TAG, "displayNotification: eventType: " + eventType);
        Log.i(TAG, "displayNotification: nsound: " + nsound);
        Log.i(TAG, "displayNotification: showNotification: " + showNotification);
        Log.i(TAG, "displayNotification: sound: " + sound);
        Log.i(TAG, "displayNotification: lights: " + lights);


        if (!showNotification) {
            return;
        }

        Integer notificationId = NotificationBuilder.createNotifIdIfNecessary(id, target);

        // defineChannelData
        String channelId = NotificationBuilder.defineChannelId(activityOrServiceContext, nsound);
        String channelName = NotificationBuilder.defineChannelName(activityOrServiceContext, nsound);
        Uri defaultSoundUri = NotificationBuilder.defineSoundUri(nsound);

        // defineTitleAndText()
        String title = NotificationBuilder.defineNotificationTitle(eventType, target, name, groupName);
        Log.d(TAG, "Notification title: " + title);
        String text = NotificationBuilder.defineNotificationText(eventType, name, message);


        // find previous messages and update notification id (if necessary)
        StatusBarNotification[] statusBarNotifications = NotificationUtils.getStatusBarNotifications(appContext);
        List<String> msgs = new ArrayList<String>();
        notificationId = NotificationBuilder.findPreviousMessagesAndUpdateNotifId(target, notificationId,
                statusBarNotifications, msgs);
        if (msgs.size() == 0) {
            Log.i("vnc", "no notifications in Status bar, when message: " + message);
        }

        msgs.add(text);

        // fill messaging style object
        NotificationCompat.MessagingStyle messagingStyle = NotificationBuilder.defineMessagingStyle(title, msgs);

        //create Notification PendingIntent
        PendingIntent pendingIntent = NotificationBuilder.createNotifPendingIntent(activityOrServiceContext,
                target, notificationId, "vncEventType", "chat");

        // createNotification
        NotificationCompat.Builder notificationBuilder = NotificationBuilder.createNotification(activityOrServiceContext, channelId, nsound,
                title, text, messagingStyle, pendingIntent, defaultSoundUri);

        // Add actions
        NotificationBuilder.addActionsForNotification(activityOrServiceContext, appContext, id,
                notificationId, notificationBuilder, target);

        //
        NotificationBuilder.setNotificationSmallIcon(activityOrServiceContext, notificationBuilder);
        NotificationBuilder.setNotificationSound(activityOrServiceContext, notificationBuilder, nsound, sound);
        NotificationBuilder.setNotificationLights(notificationBuilder, lights);
        NotificationBuilder.setNotificationColor(activityOrServiceContext, notificationBuilder);

        Notification notification = notificationBuilder.build();

        //saveDataInNotification
        notification.extras.putStringArrayList(PREVIOUS_MESSAGES, (ArrayList<String>) msgs);
        notification.extras.putInt(NOTIFY_ID_FOR_UPDATING, notificationId);
        notification.extras.putString(MESSAGE_TARGET, target);

        //
        NotificationBuilder.setNotificationImageRes(activityOrServiceContext, notification);
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) activityOrServiceContext.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationBuilder.createNotificationChannel(notificationManager, channelId, channelName, nsound);

        //
        notificationManager.notify(notificationId, notification);

        //
        NotificationUtils.saveNotificationsIdInFile(activityOrServiceContext, target, notificationId);
    }


}
