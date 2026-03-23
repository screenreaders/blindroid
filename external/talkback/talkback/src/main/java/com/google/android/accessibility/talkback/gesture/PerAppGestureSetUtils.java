/*
 * Copyright 2026
 */
package com.google.android.accessibility.talkback.gesture;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import com.google.android.accessibility.talkback.R;

/** Utility for storing per-app gesture set overrides. */
public final class PerAppGestureSetUtils {

  private PerAppGestureSetUtils() {}

  public static final int SET_GLOBAL = -1;
  public static final int SET_DEFAULT = 0;
  public static final int SET_IOS = 1;

  public static String getPrefKey(Context context, String packageName) {
    return context.getString(R.string.pref_per_app_gesture_set_prefix) + packageName;
  }

  public static int getGestureSetForPackage(
      SharedPreferences prefs, Context context, @Nullable String packageName) {
    if (TextUtils.isEmpty(packageName)) {
      return SET_GLOBAL;
    }
    String key = getPrefKey(context, packageName);
    String value = prefs.getString(key, null);
    if (TextUtils.isEmpty(value)) {
      return SET_GLOBAL;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return SET_GLOBAL;
    }
  }

  public static void setGestureSetForPackage(
      SharedPreferences prefs, Context context, @Nullable String packageName, int gestureSet) {
    if (TextUtils.isEmpty(packageName)) {
      return;
    }
    String key = getPrefKey(context, packageName);
    SharedPreferences.Editor editor = prefs.edit();
    if (gestureSet == SET_GLOBAL) {
      editor.remove(key);
    } else {
      editor.putString(key, String.valueOf(gestureSet));
    }
    editor.apply();
  }
}
