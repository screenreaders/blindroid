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

package com.google.android.accessibility.talkback.quickmenu;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping;
import com.google.android.accessibility.talkback.plugin.PluginRegistry;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Preference helpers for BlindReader quick menu. */
public final class QuickMenuPreferences {

  private QuickMenuPreferences() {}

  public static List<String> getQuickMenuActions(Context context) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    if (!isCustomized(context, prefs)) {
      return getDefaultActions(context);
    }
    List<String> actions = new ArrayList<>();
    for (String actionKey : getSupportedActions(context)) {
      if (PluginRegistry.isPluginActionKey(actionKey)) {
        String key = buildActionPrefKey(context, actionKey);
        if (!prefs.contains(key) || prefs.getBoolean(key, false)) {
          actions.add(actionKey);
        }
      } else if (prefs.getBoolean(buildActionPrefKey(context, actionKey), false)) {
        actions.add(actionKey);
      }
    }
    return actions;
  }

  public static List<String> getSupportedActions(Context context) {
    List<String> actions = new ArrayList<>(GestureShortcutMapping.getAllActionKeys(context));
    actions.addAll(PluginRegistry.getQuickActionKeys(context));
    return actions;
  }

  public static Set<String> getDefaultActionSet(Context context) {
    List<String> defaults = getDefaultActions(context);
    return new HashSet<>(defaults);
  }

  public static List<String> getDefaultActions(Context context) {
    String[] actionKeys =
        context.getResources().getStringArray(R.array.pref_quick_menu_action_defaults);
    LinkedHashSet<String> defaults = new LinkedHashSet<>();
    for (String actionKey : actionKeys) {
      if (!TextUtils.isEmpty(actionKey)
          && !TextUtils.equals(actionKey, context.getString(R.string.shortcut_value_unassigned))) {
        defaults.add(actionKey);
      }
    }
    defaults.addAll(PluginRegistry.getQuickActionKeys(context));
    return new ArrayList<>(defaults);
  }

  public static boolean isActionEnabled(Context context, String actionKey) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    if (!isCustomized(context, prefs)) {
      return getDefaultActionSet(context).contains(actionKey);
    }
    if (PluginRegistry.isPluginActionKey(actionKey)) {
      String prefKey = buildActionPrefKey(context, actionKey);
      if (!prefs.contains(prefKey)) {
        return true;
      }
    }
    return prefs.getBoolean(buildActionPrefKey(context, actionKey), false);
  }

  public static void markCustomized(Context context) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    prefs.edit().putBoolean(context.getString(R.string.pref_quick_menu_customized_key), true).apply();
  }

  public static void resetToDefaults(Context context) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    SharedPreferences.Editor editor = prefs.edit();
    for (String actionKey : getSupportedActions(context)) {
      editor.remove(buildActionPrefKey(context, actionKey));
    }
    editor.putBoolean(context.getString(R.string.pref_quick_menu_customized_key), false);
    editor.apply();
  }

  public static boolean isCustomized(Context context, SharedPreferences prefs) {
    return prefs.getBoolean(context.getString(R.string.pref_quick_menu_customized_key), false);
  }

  public static String buildActionPrefKey(Context context, String actionKey) {
    return context.getString(R.string.pref_quick_menu_action_prefix) + actionKey;
  }

  public static String getActionLabel(Context context, String actionKey) {
    if (PluginRegistry.isPluginActionKey(actionKey)) {
      return PluginRegistry.getActionLabel(context, actionKey);
    }
    return GestureShortcutMapping.getActionString(context, actionKey);
  }
}
