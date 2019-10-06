package org.apache.cordova.firebase;

import android.content.Context;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.apache.cordova.firebase.utils.SharedPrefsUtils;

import java.util.Map;

public class FirebasePluginMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FirebasePluginMessagingService";

    private PayloadProcessor payloadProcessor;


    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.i(TAG, "onMessageReceived" + remoteMessage);

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
            Log.i(TAG, "Message was handled by a registered receiver");
            return;
        }

        Map<String, String> payload = remoteMessage.getData();
        Log.i(TAG, "payload 'data' received" + payload);

        if (payloadProcessor == null) {
            payloadProcessor = new PayloadProcessor(this, getApplicationContext());
        }

        if (payload.get("vnc") != null) {
            payloadProcessor.processTalkPayload(payload);
        } else if (payload.get("vnctask") != null) {
            String mApiKey = SharedPrefsUtils.getString(getApplicationContext(), "redmine-api-key");
            if (mApiKey == null) {
                return;
            }
            payloadProcessor.processTaskPayload(payload);
        } else if (payload.containsKey("subject") && payload.containsKey("fromAddress")) {
            payloadProcessor.processMailPayload(payload);
        }
    }
}
