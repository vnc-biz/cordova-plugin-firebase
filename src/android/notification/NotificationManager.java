package org.apache.cordova.firebase.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.os.Bundle;

import org.apache.cordova.firebase.utils.NotificationUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class NotificationManager {

    private static final String TAG = "NotificationDisplay";

    private static final String PREVIOUS_MESSAGES = "previousMessages";
    private static final String NOTIFY_ID_FOR_UPDATING = "notifIdForUpdating";
    private static final String MESSAGE_TARGET = "messageTarget";
    private static final String MESSAGE_ID = "messageId";
    private static final String CONV_ID = "convId";

    private static final String PREFS_NOTIF_COUNTER = "notificationCounter";
    private static final String PREFS_STRING_SET_KEY = "previousNotifications";

    private static long timeFromPrevNotify = 0;

    synchronized public static void displayMailNotification(Context activityOrServiceContext, Context appContext, String subject,
        String body, String fromDisplay, String msgId,  String type, String folderId, String sound, String fromAddress, String cId) {

        if (checkIfNotificationExist(appContext, msgId)) {
          Log.i(TAG, "Notification EXIST = " + msgId + ", so ignore it");
          return;
        }


        android.app.NotificationManager notificationManager = (android.app.NotificationManager) activityOrServiceContext.getSystemService(Context.NOTIFICATION_SERVICE);

        Integer notificationId = msgId.hashCode();
        Log.i(TAG, "displayMailNotification: subject:" + subject + ", body: " + body + ", fromDisplay: " + fromDisplay + ", msgId: " + msgId + ", type: " + type + ", notificationId: " + notificationId + ", cId: " + cId);
        // defineChannelData
        String nsound = sound.equals("false") ? "mute" : "";
        String channelId = NotificationCreator.defineChannelId(activityOrServiceContext, nsound);
        String channelName = NotificationCreator.defineChannelName(activityOrServiceContext, nsound);
        Uri defaultSoundUri = NotificationCreator.defineSoundUri(nsound);

        //create Notification PendingIntent
        PendingIntent pendingIntent = NotificationCreator.createNotifPendingIntentMail(activityOrServiceContext, msgId, notificationId, type, folderId, cId);

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle(
        new NotificationCompat.Builder(activityOrServiceContext, channelId)
        .setContentTitle(fromDisplay)
        .setContentText(body)
        );

        bigTextStyle.setBigContentTitle(fromDisplay);
        bigTextStyle.bigText(body);

        NotificationCompat.Builder notificationBuilder = NotificationCreator.createNotification(activityOrServiceContext, channelId, nsound,
        fromDisplay, body, bigTextStyle, pendingIntent, defaultSoundUri);

        NotificationCreator.addReplyMailAction(activityOrServiceContext, appContext, notificationId, notificationBuilder, msgId, subject, fromAddress, fromDisplay);
        NotificationCreator.addMarkMailAsReadAction(activityOrServiceContext, appContext, notificationId, notificationBuilder, msgId);
        NotificationCreator.addDeleteMailAction(activityOrServiceContext, appContext, notificationId, notificationBuilder, msgId);

        NotificationCreator.setNotificationSmallIcon(activityOrServiceContext, notificationBuilder);
        // NotificationCreator.setNotificationSound(activityOrServiceContext, notificationBuilder, nsound, sound);
        // NotificationCreator.setNotificationLights(notificationBuilder, lights);
        NotificationCreator.setNotificationColor(activityOrServiceContext, notificationBuilder);

        Notification notification = notificationBuilder.build();

        notification.extras.putString(MESSAGE_ID, msgId);
        notification.extras.putString(CONV_ID, cId);

        Log.i(TAG, "displayMailNotification: channelId: " + channelId + ", channelName: " + channelName + ", defaultSoundUri: " + defaultSoundUri);
        Log.i(TAG, "displayMailNotification: display notificationId: " + notificationId);

        //
        NotificationCreator.setNotificationImageRes(activityOrServiceContext, notification);
        NotificationCreator.createNotificationChannel(notificationManager, channelId, channelName, nsound);

        notificationManager.notify(notificationId, notification);
    }

    synchronized public static void displayTaskNotification(Context activityOrServiceContext, Context appContext,
                                                          String body, String username, String taskId, String taskUpdatedOn,
                                                          String type, String sound, String open_in_browser, String language) {
        Log.i(TAG, "displayTaskNotification: body: " + body + ", username: " + username + ", taskId: " + taskId + ", taskUpdatedOn: " + taskUpdatedOn + ", type: " + type);

        android.app.NotificationManager notificationManager = (android.app.NotificationManager) activityOrServiceContext.getSystemService(Context.NOTIFICATION_SERVICE);

        Integer notificationId = (taskId + body).hashCode();

        // defineChannelData
        String nsound = sound.equals("false") ? "mute" : "";
        String channelId = NotificationCreator.defineChannelId(activityOrServiceContext, nsound);
        String channelName = NotificationCreator.defineChannelName(activityOrServiceContext, nsound);
        Uri defaultSoundUri = NotificationCreator.defineSoundUri(nsound);

        //create Notification PendingIntent
        PendingIntent pendingIntent = NotificationCreator.createNotifPendingIntentTask(activityOrServiceContext,
                taskId, notificationId, "vncTaskEventType", taskUpdatedOn, type, open_in_browser);

        String title;
        if (language.equals("en")) {
            if (type.equals("assignment")) {
                title = "Task assignment";
            } else if (type.equals("task_update")) {
                title = "Task updated";
            } else if (type.equals("reminder")) {
                title = "Task reminder";
            } else if (type.equals("mentioning")) {
                title = "Task mention";
            } else if (type.equals("overdue_tasks")) {
                title = "Task overdue";
            } else if (type.equals("deletion")) {
                title = "Task deleted";
            } else if (type.equals("assignment_removed")) {
                title = "Task removed";
            } else {
                title = "Task notification";
            }
        } else {
            if (type.equals("assignment")) {
                title = "Aufgabenzuweisung";
            } else if (type.equals("task_update")) {
                title = "Aufgabenaktualisierung";
            } else if (type.equals("reminder")) {
                title = "Aufgabenerinnerung";
            } else if (type.equals("mentioning")) {
                title = "Erwähnung in Aufgaben";
            } else if (type.equals("overdue_tasks")) {
                title = "Überfällige Aufgaben";
            } else if (type.equals("deletion")) {
                title = "Aufgabe(n) gelöscht";
            } else if (type.equals("assignment_removed")) {
                title = "Aufgabe(n) entfernt";
            } else {
                title = "Aufgabenbenachrichtigung";
            }
        }

        NotificationCompat.Builder notificationBuilder = NotificationCreator.createNotification(activityOrServiceContext, channelId, nsound,
                title, body, null, pendingIntent, defaultSoundUri);

        if (type.equals("reminder")) {
            NotificationCreator.addSnoozeAction(activityOrServiceContext, appContext,
                  notificationId, notificationBuilder, taskId);
        }

        NotificationCreator.setNotificationSmallIcon(activityOrServiceContext, notificationBuilder);
        // NotificationCreator.setNotificationSound(activityOrServiceContext, notificationBuilder, nsound, sound);
        // NotificationCreator.setNotificationLights(notificationBuilder, lights);
        NotificationCreator.setNotificationColor(activityOrServiceContext, notificationBuilder);

        Notification notification = notificationBuilder.build();

        Log.i(TAG, "displayTaskNotification: channelId: " + channelId + ", channelName: " + channelName + ", defaultSoundUri: " + defaultSoundUri);
        Log.i(TAG, "displayTaskNotification: display notificationId: " + notificationId);

        //
        NotificationCreator.setNotificationImageRes(activityOrServiceContext, notification);
        NotificationCreator.createNotificationChannel(notificationManager, channelId, channelName, nsound);

        notificationManager.notify(notificationId, notification);
    }

    synchronized public static void displayTalkNotification(Context activityOrServiceContext, Context appContext,
                                                        String id, String msgid, String target, String name, String groupName,
                                                        String message, String eventType, String nsound,
                                                        String sound, String lights) {
        Log.i(TAG, "displayNotification: msgid: " + msgid);
        Log.i(TAG, "displayNotification: Target: " + target);
        Log.i(TAG, "displayNotification: username: " + name);
        Log.i(TAG, "displayNotification: groupName: " + groupName);
        Log.i(TAG, "displayNotification: message: " + message);
        Log.i(TAG, "displayNotification: eventType: " + eventType);
        Log.i(TAG, "displayNotification: nsound: " + nsound);
        Log.i(TAG, "displayNotification: sound: " + sound);
        Log.i(TAG, "displayNotification: lights: " + lights);

        if (checkIfNotificationExist(appContext, msgid)) {
            Log.i(TAG, "Notification EXIST = " + msgid);
            return;
        }

        Integer notificationId = NotificationCreator.createNotifIdIfNecessary(id, target);

        // defineChannelData
        String channelId = NotificationCreator.defineChannelId(activityOrServiceContext, nsound);
        String channelName = NotificationCreator.defineChannelName(activityOrServiceContext, nsound);
        Uri defaultSoundUri = NotificationCreator.defineSoundUri(nsound);

        // defineTitleAndText()
        String title = NotificationCreator.defineNotificationTitle(eventType, target, name, groupName);
        Log.d(TAG, "Notification title: " + title);
        String text = NotificationCreator.defineNotificationText(eventType, name, message);

        //
        if (timeFromPrevNotify > 0) {
            long difference = System.currentTimeMillis() - timeFromPrevNotify;
            if (difference <= 25) {
                try {
                    Thread.sleep(35);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }


        // find previous messages and update notification id (if necessary)
        StatusBarNotification[] statusBarNotifications = NotificationUtils.getStatusBarNotifications(appContext);
        List<String> msgs = new ArrayList<String>();
        Integer existingNotificationId = NotificationCreator.findNotificationIdForTargetAndUpdateContent(target, statusBarNotifications, msgs);
        if (existingNotificationId > -1) {
            notificationId = existingNotificationId;
        }

        msgs.add(text);

        // fill messaging style object
        NotificationCompat.MessagingStyle messagingStyle = NotificationCreator.defineMessagingStyle(title, msgs);

        //create Notification PendingIntent
        PendingIntent pendingIntent = NotificationCreator.createNotifPendingIntentTalk(activityOrServiceContext,
                target, notificationId, "vncEventType", "chat");

        // createNotification
        NotificationCompat.Builder notificationBuilder = NotificationCreator.createNotification(activityOrServiceContext, channelId, nsound,
                title, text, messagingStyle, pendingIntent, defaultSoundUri);

        // Add actions
        NotificationCreator.addReplyAndMarkAsReadActions(activityOrServiceContext, appContext,
                notificationId, notificationBuilder, target);

        //
        NotificationCreator.setNotificationSmallIcon(activityOrServiceContext, notificationBuilder);
        NotificationCreator.setNotificationSound(activityOrServiceContext, notificationBuilder, nsound, sound);
        NotificationCreator.setNotificationLights(notificationBuilder, lights);
        NotificationCreator.setNotificationColor(activityOrServiceContext, notificationBuilder);

        Notification notification = notificationBuilder.build();

        //saveDataInNotification
        notification.extras.putStringArrayList(PREVIOUS_MESSAGES, (ArrayList<String>) msgs);
        notification.extras.putInt(NOTIFY_ID_FOR_UPDATING, notificationId);
        notification.extras.putString(MESSAGE_TARGET, target);

        //
        NotificationCreator.setNotificationImageRes(activityOrServiceContext, notification);
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) activityOrServiceContext.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCreator.createNotificationChannel(notificationManager, channelId, channelName, nsound);

        //
        notificationManager.notify(notificationId, notification);

        //
        timeFromPrevNotify = System.currentTimeMillis();

        //
        NotificationUtils.saveNotificationsIdInFile(activityOrServiceContext, target, notificationId);
    }

    private static boolean checkIfNotificationExist(Context appContext, String msgid) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> previousNotifications = prefs.getStringSet(PREFS_STRING_SET_KEY, null);
        int counter = prefs.getInt(PREFS_NOTIF_COUNTER, 0);
        String stringNotificationId = msgid;
        long currentTime = System.currentTimeMillis();

        if (previousNotifications != null && previousNotifications.size() > 0) {
            //Checking notifications on time to expire
            long hour = 1000 * 60 * 60;
            if (counter > 100) {
                editor.putInt(PREFS_NOTIF_COUNTER, 0);
                Set<String> curNotif = new HashSet<String>();
                Iterator<String> iter = previousNotifications.iterator();
                while (iter.hasNext()) {
                    String prevNotif = iter.next();
                    long timeNotif = prefs.getLong(prevNotif, 0);
                    if (timeNotif != 0 && currentTime - timeNotif > hour) {
                        //remove timeStamp for given notificationId
                        editor.remove(prevNotif).apply();
                        //removed notificationId from Set
                        iter.remove();
                    } else {
                        curNotif.add(prevNotif);
                    }
                }
                previousNotifications = curNotif;
            } else {
                editor.putInt(PREFS_NOTIF_COUNTER, ++counter);
            }
            //Check if notificationId already exist in set
            if (previousNotifications.contains(stringNotificationId)) {
                return true;
            } else {
                //add to set, and create record in prefs with timestamp
                writeNotificationToPrefs(stringNotificationId, currentTime, previousNotifications, editor);
            }
        } else {
            //add to set, and create record in prefs with timestamp
            writeNotificationToPrefs(stringNotificationId, currentTime, new HashSet<String>(), editor);
        }
        return false;
    }

    private static void writeNotificationToPrefs(String notificationId, long currentTime, Set<String> existedSet,
                                                 SharedPreferences.Editor editor) {
        existedSet.add(notificationId);
        editor.putStringSet(PREFS_STRING_SET_KEY, existedSet).apply();
        editor.putLong(notificationId, currentTime).apply();
    }

    public static void hideMailNotificationsForMid(Context activityOrServiceContext, String mid) {
        try {
            StatusBarNotification[] statusBarNotifications = NotificationUtils.getStatusBarNotifications(activityOrServiceContext);
            android.app.NotificationManager notificationManager = (android.app.NotificationManager) activityOrServiceContext.getSystemService(Context.NOTIFICATION_SERVICE);
            for (StatusBarNotification sbn : statusBarNotifications) {
                Notification curNotif = sbn.getNotification();
                Bundle bundle = curNotif.extras;
                String currentMessageId = bundle.getString(MESSAGE_ID);
                if (currentMessageId != null && currentMessageId.equals(mid)) {
                    notificationManager.cancel(sbn.getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideMailNotificationsExceptMids(Context activityOrServiceContext, List<String> mids) {
        try {
            StatusBarNotification[] statusBarNotifications = NotificationUtils.getStatusBarNotifications(activityOrServiceContext);
            android.app.NotificationManager notificationManager = (android.app.NotificationManager) activityOrServiceContext.getSystemService(Context.NOTIFICATION_SERVICE);
            for (StatusBarNotification sbn : statusBarNotifications) {
                Notification curNotif = sbn.getNotification();
                Bundle bundle = curNotif.extras;
                String currentMessageId = bundle.getString(MESSAGE_ID);
                if (currentMessageId != null && !mids.contains(currentMessageId)) {
                    notificationManager.cancel(sbn.getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideMailNotificationsForCid(Context activityOrServiceContext, String cid) {
        try {
            StatusBarNotification[] statusBarNotifications = NotificationUtils.getStatusBarNotifications(activityOrServiceContext);
            android.app.NotificationManager notificationManager = (android.app.NotificationManager) activityOrServiceContext.getSystemService(Context.NOTIFICATION_SERVICE);
            for (StatusBarNotification sbn : statusBarNotifications) {
                Notification curNotif = sbn.getNotification();
                Bundle bundle = curNotif.extras;
                String currentCId = bundle.getString(CONV_ID);
                if (currentCId != null && currentCId.equals(cid)) {
                    notificationManager.cancel(sbn.getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideMailNotificationsExceptCids(Context activityOrServiceContext, List<String> cids) {
        try {
            StatusBarNotification[] statusBarNotifications = NotificationUtils.getStatusBarNotifications(activityOrServiceContext);
            android.app.NotificationManager notificationManager = (android.app.NotificationManager) activityOrServiceContext.getSystemService(Context.NOTIFICATION_SERVICE);
            for (StatusBarNotification sbn : statusBarNotifications) {
                Notification curNotif = sbn.getNotification();
                Bundle bundle = curNotif.extras;
                String currentCId = bundle.getString(CONV_ID);
                if (currentCId != null && !cids.contains(currentCId)) {
                    notificationManager.cancel(sbn.getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
