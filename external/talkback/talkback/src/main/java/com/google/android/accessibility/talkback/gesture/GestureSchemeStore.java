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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Stores named gesture schemes in shared preferences. */
public final class GestureSchemeStore {

  private GestureSchemeStore() {}

  public static final class Scheme {
    public final String id;
    public final String name;
    public final int gestureSet;
    public final String scope;
    public final String packageName;
    public final JSONObject mappings;

    public Scheme(
        String id,
        String name,
        int gestureSet,
        String scope,
        String packageName,
        JSONObject mappings) {
      this.id = id;
      this.name = name;
      this.gestureSet = gestureSet;
      this.scope = scope;
      this.packageName = packageName;
      this.mappings = mappings;
    }
  }

  public static List<Scheme> loadSchemes(Context context) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String listKey = context.getString(R.string.pref_gesture_scheme_store_list_key);
    String listJson = prefs.getString(listKey, null);
    if (TextUtils.isEmpty(listJson)) {
      return Collections.emptyList();
    }
    List<Scheme> schemes = new ArrayList<>();
    try {
      JSONArray ids = new JSONArray(listJson);
      for (int i = 0; i < ids.length(); i++) {
        String id = ids.optString(i, null);
        if (TextUtils.isEmpty(id)) {
          continue;
        }
        Scheme scheme = loadScheme(context, id);
        if (scheme != null) {
          schemes.add(scheme);
        }
      }
    } catch (JSONException e) {
      return Collections.emptyList();
    }
    return schemes;
  }

  public static Scheme loadScheme(Context context, String id) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String prefix = context.getString(R.string.pref_gesture_scheme_store_prefix);
    String raw = prefs.getString(prefix + id, null);
    if (TextUtils.isEmpty(raw)) {
      return null;
    }
    try {
      JSONObject obj = new JSONObject(raw);
      String name = obj.optString("name", id);
      int gestureSet = obj.optInt("gestureSet", 0);
      String scope = obj.optString("scope", "global");
      String packageName = obj.optString("package", null);
      JSONObject mappings = obj.optJSONObject("mappings");
      if (mappings == null) {
        mappings = new JSONObject();
      }
      return new Scheme(id, name, gestureSet, scope, packageName, mappings);
    } catch (JSONException e) {
      return null;
    }
  }

  public static String saveScheme(Context context, JSONObject schemeJson) {
    String id = String.valueOf(System.currentTimeMillis());
    saveScheme(context, id, schemeJson);
    return id;
  }

  public static void saveScheme(Context context, String id, JSONObject schemeJson) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String prefix = context.getString(R.string.pref_gesture_scheme_store_prefix);
    prefs.edit().putString(prefix + id, schemeJson.toString()).apply();
    updateList(context, id, /* add= */ true);
  }

  public static void deleteScheme(Context context, String id) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String prefix = context.getString(R.string.pref_gesture_scheme_store_prefix);
    prefs.edit().remove(prefix + id).apply();
    updateList(context, id, /* add= */ false);
  }

  public static void linkSchemeToPackage(Context context, String packageName, String schemeId) {
    if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(schemeId)) {
      return;
    }
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String prefix = context.getString(R.string.pref_gesture_scheme_app_prefix);
    prefs.edit().putString(prefix + packageName, schemeId).apply();
  }

  public static void unlinkSchemeFromPackage(Context context, String packageName) {
    if (TextUtils.isEmpty(packageName)) {
      return;
    }
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String prefix = context.getString(R.string.pref_gesture_scheme_app_prefix);
    String lastPrefix = context.getString(R.string.pref_gesture_scheme_app_last_prefix);
    prefs.edit().remove(prefix + packageName).remove(lastPrefix + packageName).apply();
  }

  public static @Nullable String getLinkedSchemeId(Context context, String packageName) {
    if (TextUtils.isEmpty(packageName)) {
      return null;
    }
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String prefix = context.getString(R.string.pref_gesture_scheme_app_prefix);
    return prefs.getString(prefix + packageName, null);
  }

  public static boolean applyLinkedSchemeIfNeeded(
      Context context, SharedPreferences prefs, @Nullable String packageName) {
    if (TextUtils.isEmpty(packageName)) {
      return false;
    }
    String schemeId = getLinkedSchemeId(context, packageName);
    if (TextUtils.isEmpty(schemeId)) {
      return false;
    }
    String lastPrefix = context.getString(R.string.pref_gesture_scheme_app_last_prefix);
    String lastApplied = prefs.getString(lastPrefix + packageName, null);
    if (schemeId.equals(lastApplied)) {
      return false;
    }
    Scheme scheme = loadScheme(context, schemeId);
    if (scheme == null) {
      return false;
    }
    boolean applied = applySchemeToPackage(context, prefs, scheme, packageName);
    if (applied) {
      prefs.edit().putString(lastPrefix + packageName, schemeId).apply();
    }
    return applied;
  }

  private static boolean applySchemeToPackage(
      Context context, SharedPreferences prefs, Scheme scheme, String packageName) {
    if (scheme == null || TextUtils.isEmpty(packageName)) {
      return false;
    }
    Set<String> allowedKeys = new HashSet<>();
    String[] gesturePrefKeys = context.getResources().getStringArray(R.array.pref_shortcut_keys);
    for (String key : gesturePrefKeys) {
      allowedKeys.add(key);
    }
    Set<String> allowedActions = new HashSet<>(GestureShortcutMapping.getAllActionKeys(context));
    allowedActions.add(context.getString(R.string.shortcut_value_unassigned));

    SharedPreferences.Editor editor = prefs.edit();
    Iterator<String> it = scheme.mappings.keys();
    while (it.hasNext()) {
      String baseKey = it.next();
      if (!allowedKeys.contains(baseKey)) {
        continue;
      }
      String action = scheme.mappings.optString(baseKey, null);
      if (TextUtils.isEmpty(action) || !allowedActions.contains(action)) {
        continue;
      }
      String scopedKey =
          context.getString(R.string.pref_gesture_scope_pkg_prefix) + packageName + ":" + baseKey;
      String prefKey =
          GestureShortcutMapping.getPrefKeyWithGestureSet(scopedKey, scheme.gestureSet);
      editor.putString(prefKey, action);
    }
    editor.putBoolean(
        context.getString(R.string.pref_per_app_gesture_set_enabled_key), true);
    editor.apply();
    PerAppGestureSetUtils.setGestureSetForPackage(
        prefs, context, packageName, scheme.gestureSet);
    return true;
  }

  private static void updateList(Context context, String id, boolean add) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String listKey = context.getString(R.string.pref_gesture_scheme_store_list_key);
    List<String> ids = new ArrayList<>();
    String listJson = prefs.getString(listKey, null);
    if (!TextUtils.isEmpty(listJson)) {
      try {
        JSONArray array = new JSONArray(listJson);
        for (int i = 0; i < array.length(); i++) {
          String existing = array.optString(i, null);
          if (!TextUtils.isEmpty(existing)) {
            ids.add(existing);
          }
        }
      } catch (JSONException e) {
        ids.clear();
      }
    }
    if (add) {
      if (!ids.contains(id)) {
        ids.add(id);
      }
    } else {
      ids.remove(id);
    }
    JSONArray out = new JSONArray();
    for (String entry : ids) {
      out.put(entry);
    }
    prefs.edit().putString(listKey, out.toString()).apply();
  }

  public static JSONObject buildSchemeJson(
      String name,
      int gestureSet,
      String scope,
      String packageName,
      JSONObject mappings) throws JSONException {
    JSONObject obj = new JSONObject();
    obj.put("version", 1);
    obj.put("name", name);
    obj.put("gestureSet", gestureSet);
    obj.put("scope", scope);
    if (!TextUtils.isEmpty(packageName)) {
      obj.put("package", packageName);
    }
    obj.put("mappings", mappings);
    return obj;
  }

  public static JSONObject cloneMappings(JSONObject original) throws JSONException {
    JSONObject out = new JSONObject();
    Iterator<String> keys = original.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      out.put(key, original.optString(key, null));
    }
    return out;
  }
}
