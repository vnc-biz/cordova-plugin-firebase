package org.apache.cordova.firebase.utils;

import android.content.Context;

public class StringUtils {

    public static String getStringResource(Context context, String name) {
        return context.getString(
                context.getResources().getIdentifier(
                        name, "string", context.getPackageName()
                )
        );
    }
}
