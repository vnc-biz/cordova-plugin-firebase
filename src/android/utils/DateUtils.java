package org.apache.cordova.firebase.utils;

import android.content.Context;
import android.util.Log;

public class DateUtils {
    static final String TAG = "DateUtils";

    public static long getCorrectedTime(Context context) {
        long currentTime = System.currentTimeMillis() / 1000;

        try {
            long timeDiff = SharedPrefsUtils.getLong(context, "time-diff-interval");

            if (timeDiff != -1) {
                currentTime = currentTime + timeDiff;
                Log.i(TAG, "Correct time, diff = " + timeDiff + ", correctedTime = " + currentTime);
            }
        } catch (Exception e){
            Log.i(TAG, "Error during parse diff: " + e.getMessage());
        }

        return currentTime;
    }
}