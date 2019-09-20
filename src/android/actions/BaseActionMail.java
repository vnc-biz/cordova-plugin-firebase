package org.apache.cordova.firebase.actions;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.apache.cordova.firebase.utils.NotificationUtils;
import org.apache.cordova.firebase.utils.SharedPrefsUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;


public abstract class BaseActionMail extends BaseAction {
    private static final String TAG = "Firebase.BaseActionMail";

    private static final String TOKEN_CONSTANT = "auth-token";

    protected String mToken;

    BaseActionMail(int notificationId, Context context, String urlSupplement) {
        super(notificationId, context, urlSupplement);

        initToken();
    }

    private void initToken() {
        mToken = SharedPrefsUtils.getString(this.context, TOKEN_CONSTANT);
    }

    @Override
    public void run() {
        Log.i(TAG, "Token : " + mToken);
        Log.i(TAG, "NotificationId : " + notificationId);
        Log.i(TAG, "Api Url : " + mApiUrl);

        super.run();
    }

    @Override
    protected void setRequestHeaders(HttpURLConnection urlConnection) throws IOException {
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setRequestProperty("Accept", "application/json");
        urlConnection.setRequestProperty("Authorization", mToken);
    }

    @Override
    protected void setRequestMethod(HttpURLConnection urlConnection) throws IOException {
        urlConnection.setRequestMethod("POST");
    }
}
