package org.apache.cordova.firebase.utils;

import android.util.Log;

import io.sentry.Sentry;
import io.sentry.event.EventBuilder;

public class SentryUtils {
    private static final String TAG = "FirebasePlugin.SentryUtils";
    private static final String SENTRY_URL = "https://6d65e128f84b474c83c7004445176498@sentry2.vnc.biz/2";

    public static void fireEvent(String name, String value) {
        try {
            checkInit();

            Sentry.capture(new EventBuilder().withTag(name, value).getEvent());
        } catch (Exception e) {
            Log.w(TAG, "Fire Sentry event error: " + e.getMessage());
        }
    }

    public static void fireMessage(String message) {
        try {
            checkInit();

            Sentry.capture(message);
        } catch (Exception e) {
            Log.w(TAG, "Fire Sentry message error: " + e.getMessage());
        }
    }

    private static void checkInit() {
        if (!Sentry.isInitialized()) {
            Sentry.init(SENTRY_URL);
            Sentry.capture("Init Sentry");
        }
    }
}