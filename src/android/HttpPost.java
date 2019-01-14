package org.apache.cordova.firebase;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.cordova.firebase.notification.NotificationDisplayManager;
import org.apache.cordova.firebase.utils.NotificationUtils;
import org.apache.cordova.firebase.utils.SharedPrefsUtils;
import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

class HttpPost implements Runnable {
    private String body;
    private String sender;
    private Context context;
    private int notificationId;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private String mToken;
    private String mApiUrl;
    private RequestType requestType;
    private static final String TOKEN_CONSTANT = "auth-token";
    private static final String API_URL = "apiUrl";

    // constructor for 'inline reply request'
    HttpPost(String body, String sender, int notificationId, Context context) {
        this.body = body;
        this.sender = sender;
        this.context = context;
        this.notificationId = notificationId;
        this.requestType = RequestType.INLINE_REPLY;

        chooseAndSetApiUrl();
        initToken();
        initNotificationObjects();
    }

    // constructor for 'mark as read request'
    HttpPost(String sender, int notificationId, Context context) {
        this.sender = sender;
        this.context = context;
        this.notificationId = notificationId;
        this.requestType = RequestType.MARK_AS_READ;

        chooseAndSetApiUrl();
        initToken();
        initNotificationObjects();
    }

    private void initNotificationObjects() {
        notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(this.context);
    }

    private void initToken() {
        mToken = SharedPrefsUtils.getString(this.context, TOKEN_CONSTANT);
    }

    private void chooseAndSetApiUrl() {
        String baseApiUrl = baseApiUrl();
        if (this.requestType == RequestType.INLINE_REPLY) {
            mApiUrl = baseApiUrl + "/xmpp-rest";
        } else if (this.requestType == RequestType.MARK_AS_READ) {
            mApiUrl = baseApiUrl + "/markConversationsRead";
        }
    }

    private String baseApiUrl() {
        return SharedPrefsUtils.getString(this.context, API_URL);
    }

    @Override
    public void run() {
        try {
            Log.i("VNC", "Token : " + mToken);
            Log.i("VNC", "notificationId : " + notificationId);
            Log.i("VNC", "To : " + sender);
            if (requestType == RequestType.INLINE_REPLY) {
                Log.i("VNC", "Message body : " + body);
            }
            Log.i("VNC", "Api Url : " + mApiUrl);

            JSONObject postData = new JSONObject();
            if (requestType == RequestType.INLINE_REPLY) {
                postData.put("target", sender);
                postData.put("messagetext", body);
            } else if (requestType == RequestType.MARK_AS_READ) {
                postData.put(sender, new Date().getTime() / 1000);
            }

            Log.i("VNC", "postData : " + postData);

            URL url = new URL(mApiUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Authorization", mToken);
            urlConnection.setRequestMethod("POST");
            int resID = context.getResources().getIdentifier("logo", "drawable", context.getPackageName());
            if (resID != 0) {
                notificationBuilder.setSmallIcon(resID);
            } else {
                notificationBuilder.setSmallIcon(context.getApplicationInfo().icon);
            }

            if (postData != null) {
                OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
                writer.write(postData.toString());
                writer.flush();
            }
            int statusCode = urlConnection.getResponseCode();
            Log.i("VNC", "Server response, statusCode: " + statusCode);
            if (statusCode == 200) {
                notificationManager.cancel(notificationId);
            } else {
                if (requestType == RequestType.INLINE_REPLY) {
                    this.saveInlineReplyOnError(context, sender, body);
                } else if (requestType == RequestType.MARK_AS_READ) {
                    this.saveMarkAsReadOnError(context, sender);
                }
                notificationManager.cancel(notificationId);
            }
        } catch (Exception e) {
            Log.i("VNC", e.getLocalizedMessage());
            if (requestType == RequestType.INLINE_REPLY) {
                this.saveInlineReplyOnError(context, sender, body);
            } else if (requestType == RequestType.MARK_AS_READ) {
                this.saveMarkAsReadOnError(context, sender);
            }
            notificationManager.cancel(notificationId);
        } finally {
            if (requestType == RequestType.MARK_AS_READ) {
                // hide all other notifications for this target
                ArrayList<String> nIds = NotificationUtils.removeFromFileAndHideNotificationsForTarget(context, sender);
                if (nIds != null) {
                    for (int i = 0; i < nIds.size(); i++) {
                        notificationManager.cancel(Integer.parseInt(nIds.get(i)));
                    }
                }

                Bundle data = new Bundle();
                data.putString("target", sender);
                FirebasePlugin.sendNotificationMarkAsRead(data);
            }
        }
    }

    private void saveInlineReplyOnError(Context context, String target, String message) {
        Log.i("VNC", "saveInlineReplyOnError, target: " + target + ", message: " + message);

        Gson gson = new Gson();
        String data = SharedPrefsUtils.getString(context, "replyMessages");
        ArrayList<Message> list = new ArrayList();
        if (data != null) {
            Type type = new TypeToken<ArrayList<Message>>() {
            }.getType();
            list = gson.fromJson(data, type);
        }
        list.add(new Message(target, message));
        String json = gson.toJson(list);

        SharedPrefsUtils.putString(context, "replyMessages", json);
    }

    private void saveMarkAsReadOnError(Context context, String target) {
        Log.i("VNC", "saveMarkAsReadOnError, target: " + target);

        Gson gson = new Gson();
        String data = SharedPrefsUtils.getString(context, "markAsReadFailedRequests");
        ArrayList<MarkAsRead> list = new ArrayList();
        if (data != null) {
            Type type = new TypeToken<ArrayList<MarkAsRead>>() {
            }.getType();
            list = gson.fromJson(data, type);
        }
        list.add(new MarkAsRead(target));
        String json = gson.toJson(list);
        SharedPrefsUtils.putString(context, "markAsReadFailedRequests", json);
    }

    private class Message {
        String target;
        String message;

        public Message(String target, String message) {
            this.target = target;
            this.message = message;
        }
    }

    private class MarkAsRead {
        String target;
        long timestamp;

        public MarkAsRead(String target) {
            this.target = target;
            this.timestamp = new Date().getTime();
        }
    }

    enum RequestType {
        INLINE_REPLY, MARK_AS_READ;
    }
}
