package org.apache.cordova.firebase;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;


import org.json.JSONObject;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

class HttpPost implements Runnable {
    private String body;
    private String sender;
    private Context context;
    private int notificationId;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private String mToken;
    private String mApiUrl;
    private static final String TOKEN_CONSTANT = "auth-token";
    private static final String API_URL = "apiUrl";


    HttpPost(String body, String sender, int notificationId, Context context) {
        this.body = body;
        this.sender = sender;
        this.context = context;
        this.notificationId = notificationId;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(context);
        mToken = getPreference(context, TOKEN_CONSTANT);
        mApiUrl = getPreference(context, API_URL);
    }

    @Override
    public void run() {
        try {
            Log.i("VNC", "Token : " + mToken);
            Log.i("VNC", "notificationId : " + notificationId);
            Log.i("VNC", "To : " + sender);
            Log.i("VNC", "Message : " + body);
            Log.i("VNC", "Api Url : " + mApiUrl);

            JSONObject postData = new JSONObject();
            postData.put("target", sender);
            postData.put("messagetext", body);
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
            Log.i("VNC", "statusCode: " + statusCode);
            if (statusCode == 200) {
                Log.i("VNC", "Success");
                Log.i("VNC", "Cancel notificationId : " + notificationId);
               notificationManager.cancel(notificationId);
                Log.i("VNC", "Done");
            } else {
                this.insertInlineReply(context, sender, body);
                notificationManager.cancel(notificationId);
            }


        } catch (Exception e) {
            Log.i("VNC", e.getLocalizedMessage());
            this.insertInlineReply(context, sender, body);
            notificationManager.cancel(notificationId);
        }
    }

    private String getPreference(Context context, String key) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getString(key, null);
    }

    private void insertInlineReply(Context context, String target, String message) {
        Gson gson = new Gson();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        String data = preferences.getString("replyMessages", null);
        ArrayList<Message> list = new ArrayList();
        if (data != null) {
            Type type = new TypeToken<ArrayList<Message>>() {}.getType();
            list = gson.fromJson(data, type);
        }
        list.add(new Message(target, message));
        String json = gson.toJson(list);
        editor.putString("replyMessages", json);
        editor.apply();
    }

    private class Message {
        String target;
        String message;
        public Message(String target,String message) {
            this.target = target;
            this.message = message;
        }
    }


}





