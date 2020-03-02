package org.apache.cordova.firebase.utils;

import android.content.Context;

import org.apache.cordova.firebase.actions.LogFCMPluginAction;

import java.util.concurrent.Executors;

public class FcmLoggerUtils {
    public static void logFcmReceived(Context context, String msgId){
        Executors.newSingleThreadExecutor().submit(new LogFCMPluginAction(context, msgId));
    }
}