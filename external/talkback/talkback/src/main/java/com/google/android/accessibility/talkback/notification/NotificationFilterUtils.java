/*
 * Copyright (C) 2026 The Blindroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.accessibility.talkback.notification;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import com.google.android.accessibility.talkback.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Helpers for per-app notification speech filtering. */
public final class NotificationFilterUtils {

  private NotificationFilterUtils() {}

  public static boolean isFilterEnabled(Context context, SharedPreferences prefs) {
    return prefs.getBoolean(
        context.getString(R.string.pref_notification_filter_enabled_key),
        context.getResources().getBoolean(R.bool.pref_notification_filter_enabled_default));
  }

  public static String getPrefKey(Context context, String packageName) {
    return context.getString(R.string.pref_notification_filter_prefix) + packageName;
  }

  public static boolean isPackageMuted(
      SharedPreferences prefs, Context context, String packageName) {
    if (TextUtils.isEmpty(packageName) || !isFilterEnabled(context, prefs)) {
      return false;
    }
    return prefs.getBoolean(getPrefKey(context, packageName), false);
  }

  public static void setPackageMuted(
      SharedPreferences prefs, Context context, String packageName, boolean muted) {
    if (TextUtils.isEmpty(packageName)) {
      return;
    }
    String key = getPrefKey(context, packageName);
    SharedPreferences.Editor editor = prefs.edit();
    if (muted) {
      editor.putBoolean(key, true);
    } else {
      editor.remove(key);
    }
    editor.apply();
  }

  public static List<String> getMutedPackages(SharedPreferences prefs, Context context) {
    String prefix = context.getString(R.string.pref_notification_filter_prefix);
    List<String> packageNames = new ArrayList<>();
    for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
      if (!entry.getKey().startsWith(prefix)) {
        continue;
      }
      Object value = entry.getValue();
      if (value instanceof Boolean && (Boolean) value) {
        packageNames.add(entry.getKey().substring(prefix.length()));
      }
    }
    return packageNames;
  }
}
