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

package com.google.android.accessibility.talkback.gesture;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;

/** Storage helpers for gesture macros. */
public final class GestureMacroStore {

  private GestureMacroStore() {}

  public static List<String> getActionList(Context context, int macroIndex) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String key = context.getString(getListKeyResId(macroIndex));
    String json = prefs.getString(key, null);
    List<String> actions = new ArrayList<>();
    if (!TextUtils.isEmpty(json)) {
      try {
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
          String value = array.optString(i, null);
          if (!TextUtils.isEmpty(value)) {
            actions.add(value);
          }
        }
      } catch (JSONException e) {
        actions.clear();
      }
    }

    if (actions.isEmpty()) {
      List<String> legacy = getLegacyActions(context, prefs, macroIndex);
      if (!legacy.isEmpty()) {
        actions.addAll(legacy);
        saveActionList(context, macroIndex, actions);
      }
    }

    return actions;
  }

  public static void saveActionList(Context context, int macroIndex, List<String> actions) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    JSONArray array = new JSONArray();
    for (String action : actions) {
      if (!TextUtils.isEmpty(action)) {
        array.put(action);
      }
    }
    prefs.edit().putString(context.getString(getListKeyResId(macroIndex)), array.toString()).apply();
  }

  public static int getListKeyResId(int macroIndex) {
    switch (macroIndex) {
      case 1:
        return R.string.pref_macro_1_actions_list_key;
      case 2:
        return R.string.pref_macro_2_actions_list_key;
      case 3:
      default:
        return R.string.pref_macro_3_actions_list_key;
    }
  }

  private static List<String> getLegacyActions(
      Context context, SharedPreferences prefs, int macroIndex) {
    List<String> legacy = new ArrayList<>();
    int[] keys = getLegacyActionKeys(macroIndex);
    String unassigned = context.getString(R.string.shortcut_value_unassigned);
    for (int key : keys) {
      String value = prefs.getString(context.getString(key), unassigned);
      if (!TextUtils.isEmpty(value) && !TextUtils.equals(value, unassigned)) {
        legacy.add(value);
      }
    }
    return legacy;
  }

  private static int[] getLegacyActionKeys(int macroIndex) {
    switch (macroIndex) {
      case 1:
        return new int[] {
          R.string.pref_macro_1_action_1_key,
          R.string.pref_macro_1_action_2_key,
          R.string.pref_macro_1_action_3_key
        };
      case 2:
        return new int[] {
          R.string.pref_macro_2_action_1_key,
          R.string.pref_macro_2_action_2_key,
          R.string.pref_macro_2_action_3_key
        };
      case 3:
      default:
        return new int[] {
          R.string.pref_macro_3_action_1_key,
          R.string.pref_macro_3_action_2_key,
          R.string.pref_macro_3_action_3_key
        };
    }
  }
}
