package org.apache.cordova.firebase.utils;

import android.util.Log;

import io.sentry.Sentry;
import io.sentry.SentryClient;

public class SentryUtils {
    private static final String TAG = "FirebasePlugin.SentryUtils";
    private static final String SENTRY_URL = "https://6d65e128f84b474c83c7004445176498@sentry2.vnc.biz/2";

    private static SentryUtils INSTANCE = null;
    private SentryClient client;

    private SentryUtils() {
    }

    public static SentryUtils getInstance() {
        if (INSTANCE == null)
            INSTANCE = new SentryUtils();

        return INSTANCE;
    }

    public void fireEvent(String name, String value) {
        try {
            checkInit();

            client.sendMessage(String.format("%s: %s", name, value));
        } catch (Exception e) {
            Log.w(TAG, "Fire Sentry event error: " + e.getMessage());
        }
    }

    public void fireMessage(String message) {
        try {
            checkInit();

            client.sendMessage(message);
        } catch (Exception e) {
            Log.w(TAG, "Fire Sentry message error: " + e.getMessage());
        }
    }

    private void checkInit() {
        if (client == null) {
            client = Sentry.init(SENTRY_URL);
            client.sendMessage("Init Sentry");
        }
    }
}