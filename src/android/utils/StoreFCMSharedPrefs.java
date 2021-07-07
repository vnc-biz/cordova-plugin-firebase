package org.apache.cordova.firebase.utils;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

import org.apache.cordova.firebase.models.FCMMessageEntity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class StoreFCMSharedPrefs {
  private void store(Context context, FCMMessageEntity message) {
      Log.i(TAG, "StoreFCMSharedPrefs, store");

      Gson gson = new Gson();
      String data = SharedPrefsUtils.getString(context, "fcmMessages");
      ArrayList<FCMMessageEntity> list = new ArrayList();
      if (data != null && !data.isEmpty()) {
          Type type = new TypeToken<ArrayList<FCMMessageEntity>>() {
          }.getType();
          list = gson.fromJson(data, type);
      }
      list.add(message);
      String json = gson.toJson(list);

      SharedPrefsUtils.putString(context, "fcmMessages", json);
  }
}
