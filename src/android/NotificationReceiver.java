package org.apache.cordova.firebase;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String NOTIFICATION_REPLY = "NotificationReply";
    private static final String VNC_PEER_JID = "vncPeerJid";
    private static final String  NOTIFY_ID = "id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().contains(NOTIFICATION_REPLY)) {
            String[] actionParts = intent.getAction().split("@@");
            int notificationId = Integer.parseInt(actionParts[1]);
            String sender = actionParts[2];
            Log.i("VNC", "NotificationReceiver onReceive, notificationId: " + notificationId + ", sender: " + sender);

            CharSequence message = getReplyMessage(intent);
            Log.i("VNC", "NotificationReceiver onReceive,message: " + message);
            if (message != null && message.length() > 0) {
                Thread thread = new Thread(new HttpPost(message.toString(), sender, notificationId, context));
                thread.start();
            }
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
