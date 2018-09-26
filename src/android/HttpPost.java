package org.apache.cordova.firebase;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONObject;

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
    private static final String TOKEN_CONSTANT = "auth-token";

    HttpPost(String body, String sender, int notificationId, Context context) {
        this.body = body;
        this.sender = sender;
        this.context = context;
        this.notificationId = notificationId;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(context);
        mToken = getPreference(context, TOKEN_CONSTANT);
    }

    @Override
    public void run() {
        try {

            Log.i("VNC", "Token : " + mToken);
            Log.i("VNC", "To : " + sender);
            Log.i("VNC", "Message : " + body);


            JSONObject postData = new JSONObject();
            postData.put("to", sender);
            postData.put("body", body);
            postData.put("token", mToken);
            URL url = new URL("https://http-reqbin.herokuapp.com/120atb71");

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");
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
            if (statusCode == 200) {
                Log.i("VNC", "Success");
                notificationBuilder.setContentText("Your message has been successfully sent.")
                        .setContentTitle(context.getApplicationInfo().loadLabel(context.getPackageManager()));
                notificationManager.notify(notificationId, notificationBuilder.build());
            } else {
                notificationBuilder.setContentText("Something went wrong.")
                        .setContentTitle(context.getApplicationInfo().loadLabel(context.getPackageManager()));
                notificationManager.notify(notificationId, notificationBuilder.build());
            }


        } catch (Exception e) {
            Log.i("VNC", e.getLocalizedMessage());
            notificationBuilder.setContentText("Please check your internet connection.")
                    .setContentTitle(context.getApplicationInfo().loadLabel(context.getPackageManager()));
            notificationManager.notify(notificationId, notificationBuilder.build());
        }
    }

    private String getPreference(Context context, String key) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getString(key, null);
    }
}
