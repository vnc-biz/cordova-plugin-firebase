package org.apache.cordova.firebase;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import org.apache.cordova.firebase.models.PayloadTalk;
import org.apache.cordova.firebase.models.PayloadTask;
import org.apache.cordova.firebase.notification.NotificationManager;
import org.apache.cordova.firebase.utils.SharedPrefsUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirebasePluginMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FirebasePlugin";

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.i(TAG, "onMessageReceived" + remoteMessage);

        String token = SharedPrefsUtils.getString(getApplicationContext(), "auth-token");
        Log.i(TAG, "onMessageReceived token: " + token);
        if (token == null) {
            return;
        }

        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // Pass the message to the receiver manager so any registered receivers can decide to handle it
        boolean wasHandled = FirebasePluginMessageReceiverManager.onMessageReceived(remoteMessage);
        if (wasHandled) {
            Log.d(TAG, "Message was handled by a registered receiver");

            // Don't process the message in this method.
            return;
        }

        Map<String, String> payload = remoteMessage.getData();
        Log.i(TAG, "payload 'data' received" + payload);

        try {
            // talk
            if (payload.get("vnc") != null) {
                JSONArray data = new JSONArray(payload.get("vnc"));

                if (data == null || data.length() == 0) {
                    Log.d(TAG, "received empty data?");
                    return;
                }

                for (int i = 0; i < data.length(); i++) {
                    PayloadTalk notification = new Gson().fromJson(data.get(i).toString(), PayloadTalk.class);
                    String msgid = notification.msgid;
                    String target = notification.jid;
                    String username = notification.name;
                    String groupName = notification.gt;
                    String message = notification.body;
                    String eventType = notification.eType;
                    String nsound = notification.nsound;

                    if (TextUtils.isEmpty(target) || TextUtils.isEmpty(username)) {
                        Log.d(TAG, "returning due to empty 'target' or 'username' values");
                        return;
                    }

                    boolean showNotification = (FirebasePlugin.inBackground() || !FirebasePlugin.hasNotificationsCallback());
                    displayTalkNotification(this, getApplicationContext(), "0", msgid,
                            target, username, groupName, message, eventType, nsound, showNotification, "", "");
                }

            // task
            } else if (payload.get("vnctask") != null) {
                JSONObject data = new JSONObject(payload.get("vnctask"));

                // {vnctask={"task_id":46568,"type":"assignment","body":"Task \"Kskdk\" has been assigned to you by Luna Beier","username":"Luna Beier"}}
                // {vnctask={"task_id":46568,"type":"task_update","body":"Task has been updated","username":null}}
                // {vnctask={"task_id":46568,"type":"reminder","body":"A task \"Kskdk\" has a due date 03\/29\/2019","username":null}}

                if (data == null) {
                    Log.d(TAG, "received empty data?");
                    return;
                }

                PayloadTask notification = new Gson().fromJson(data.toString(), PayloadTask.class);
                String body = notification.body;
                String username = notification.username;
                String taskId = notification.task_id;
                String taskUpdatedOn = notification.task_updated_on;
                String type = notification.type;

                displayTaskNotification(this, getApplicationContext(), body, username, taskId, taskUpdatedOn, type);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
    }

    private ExecutorService notificationPool = Executors.newFixedThreadPool(1);

    public void displayTalkNotification(final Context activityOrServiceContext, final Context appContext,
                                    final String id, final String msgid, final String target,
                                    final String name, final String groupName,
                                    final String message, final String eventType, final String nsound,
                                    final boolean showNotification, final String sound, final String lights) {
        notificationPool.execute(new Runnable() {
            public void run() {
                NotificationManager.displayTalkNotification(activityOrServiceContext, appContext, id, msgid,
                        target, name, groupName, message, eventType, nsound, showNotification, sound, lights);
            }
        });
    }

    public void displayTaskNotification(final Context activityOrServiceContext, final Context appContext,
                                      final String body, final String username, final String taskId, final String taskUpdatedOn,
                                      final String type) {
        notificationPool.execute(new Runnable() {
            public void run() {
                NotificationManager.displayTaskNotification(activityOrServiceContext, appContext,
                        body, username, taskId, taskUpdatedOn, type);
            }
        });
    }

}
