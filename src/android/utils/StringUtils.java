package org.apache.cordova.firebase.utils;

import android.content.Context;

import android.os.Build;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

public class StringUtils {

    public static String getStringResource(Context context, String name) {
        return context.getString(
                context.getResources().getIdentifier(
                        name, "string", context.getPackageName()
                )
        );
    }

    public static Spannable getColorizedText(Context context, String stringResName, String colorName) {
        Spannable spannable = new SpannableString(getStringResourcestringRes(context, stringResName));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            spannable.setSpan(
                new ForegroundColorSpan(context.getColor(context.getResources().getIdentifier(colorName, "color", context.getPackageName()))),
                0, 
                spannable.length(), 
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        
        return spannable;
    }
}
