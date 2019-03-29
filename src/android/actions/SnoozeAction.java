package org.apache.cordova.firebase.actions;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.cordova.firebase.FirebasePlugin;
import org.apache.cordova.firebase.notification.NotificationCreator;
import org.apache.cordova.firebase.utils.NotificationUtils;
import org.apache.cordova.firebase.utils.SharedPrefsUtils;
import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;

public class SnoozeAction extends BaseActionTask {
    private static final String TAG = "Firebase.SnoozeAction";

    protected Date remindOn;

    public SnoozeAction(String taskId, int notificationId, Date remindOn, Context context) {
        super(taskId, notificationId, context, "/issues/" + taskId);

        this.remindOn = remindOn;
    }

    @Override
    public void run() {
        super.run();

        try {
            JSONObject putData = new JSONObject();
            //
            JSONObject remindOnData = new JSONObject();
            remindOnData.put("remind_on", remindOn.toString());
            //
            putData.put("issue", remindOnData);

            Log.i(TAG, "putData : " + putData);

            HttpURLConnection urlConnection = createUrlConnection();

            NotificationCreator.setNotificationSmallIcon(context, notificationBuilder);

            if (putData != null) {
                OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
                writer.write(putData.toString());
                writer.flush();
            }
            int statusCode = urlConnection.getResponseCode();
            Log.i(TAG, "Server response, statusCode: " + statusCode);
            if (statusCode != 200) {
                saveSnoozeOnError(context, taskId);
            }
            notificationManager.cancel(notificationId);
        } catch (Exception e) {
            Log.i(TAG, e.getLocalizedMessage());

            saveSnoozeOnError(context, taskId);
            notificationManager.cancel(notificationId);
        } finally {

        }
    }

    private void saveSnoozeOnError(Context context, String taskId) {
        Log.i(TAG, "saveSnoozeOnError, taskId: " + taskId);

        Gson gson = new Gson();
        String data = SharedPrefsUtils.getString(context, "snoozeFailedRequests");
        ArrayList<SnoozeEntity> list = new ArrayList();
        if (data != null) {
            Type type = new TypeToken<ArrayList<SnoozeEntity>>() {
            }.getType();
            list = gson.fromJson(data, type);
        }
        list.add(new SnoozeEntity(taskId));
        String json = gson.toJson(list);
        SharedPrefsUtils.putString(context, "snoozeFailedRequests", json);
    }

    private class SnoozeEntity {
        String taskId;
        long timestamp;

        public SnoozeEntity(String taskId) {
            this.taskId = taskId;
            this.timestamp = new Date().getTime();
        }
    }

}
