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

package com.google.android.accessibility.talkback.preference.base;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.gesture.GestureSchemeStore;
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping;
import com.google.android.accessibility.talkback.preference.PreferencesActivityUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

/** Manager for named gesture schemes. */
public class GestureSchemeManagerFragment extends TalkbackBaseFragment {

  private SharedPreferences prefs;
  private @Nullable String currentScopePackage;
  private @Nullable CharSequence currentScopeLabel;

  public GestureSchemeManagerFragment() {
    super(R.xml.gesture_scheme_manager_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.pref_gesture_scheme_manager_title);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    Context context = getContext();
    if (context == null) {
      return;
    }
    prefs = SharedPreferencesUtils.getSharedPreferences(context);
    resolveScopeApp(context);

    Preference savePref = findPreference(getString(R.string.pref_gesture_scheme_save_current_key));
    if (savePref != null) {
      savePref.setOnPreferenceClickListener(
          preference -> {
            showSaveDialog(context, /* existingId= */ null, /* existingName= */ null);
            return true;
          });
    }

    Preference importPref = findPreference(getString(R.string.pref_gesture_scheme_import_key));
    if (importPref != null) {
      importPref.setOnPreferenceClickListener(
          preference -> {
            showImportDialog(context);
            return true;
          });
    }

    populateSchemeList(context);
  }

  @Override
  public void onResume() {
    super.onResume();
    Context context = getContext();
    if (context == null) {
      return;
    }
    resolveScopeApp(context);
    populateSchemeList(context);
  }

  private void resolveScopeApp(Context context) {
    TalkBackService service = TalkBackService.getInstance();
    if (service == null || !TalkBackService.isServiceActive()) {
      currentScopePackage = null;
      currentScopeLabel = null;
      return;
    }
    if (service.getRootInActiveWindow() == null
        || service.getRootInActiveWindow().getPackageName() == null) {
      currentScopePackage = null;
      currentScopeLabel = null;
      return;
    }
    currentScopePackage = service.getRootInActiveWindow().getPackageName().toString();
    PackageManager pm = context.getPackageManager();
    try {
      ApplicationInfo info = pm.getApplicationInfo(currentScopePackage, 0);
      currentScopeLabel = pm.getApplicationLabel(info);
    } catch (PackageManager.NameNotFoundException e) {
      currentScopeLabel = currentScopePackage;
    }
  }

  private void populateSchemeList(Context context) {
    PreferenceCategory listCategory =
        findPreference(getString(R.string.pref_gesture_scheme_list_category_key));
    if (listCategory == null) {
      return;
    }
    listCategory.removeAll();
    List<GestureSchemeStore.Scheme> schemes = GestureSchemeStore.loadSchemes(context);
    if (schemes.isEmpty()) {
      Preference empty = new Preference(context);
      empty.setSelectable(false);
      empty.setTitle(R.string.pref_gesture_scheme_list_empty);
      listCategory.addPreference(empty);
      return;
    }
    for (GestureSchemeStore.Scheme scheme : schemes) {
      Preference pref = new Preference(context);
      pref.setTitle(scheme.name);
      pref.setSummary(buildSchemeSummary(context, scheme));
      pref.setOnPreferenceClickListener(
          preference -> {
            showSchemeActions(context, scheme);
            return true;
          });
      listCategory.addPreference(pref);
    }
  }

  private String buildSchemeSummary(Context context, GestureSchemeStore.Scheme scheme) {
    String scope =
        TextUtils.equals(scheme.scope, "current_app")
            ? (TextUtils.isEmpty(scheme.packageName) ? "current_app" : scheme.packageName)
            : "global";
    return "Set " + scheme.gestureSet + " • " + scope;
  }

  private void showSaveDialog(Context context, @Nullable String schemeId, @Nullable String name) {
    EditText input = new EditText(context);
    input.setInputType(InputType.TYPE_CLASS_TEXT);
    input.setHint(getString(R.string.pref_gesture_scheme_name_hint));
    if (!TextUtils.isEmpty(name)) {
      input.setText(name);
    }
    A11yAlertDialogWrapper.alertDialogBuilder(context)
        .setTitle(R.string.pref_gesture_scheme_save_current_title)
        .setView(input)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> {
              String value = input.getText().toString().trim();
              String schemeName =
                  TextUtils.isEmpty(value) ? getDefaultSchemeName(context) : value;
              saveCurrentScheme(context, schemeId, schemeName);
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private String getDefaultSchemeName(Context context) {
    return getString(R.string.pref_gesture_scheme_manager_title)
        + " "
        + System.currentTimeMillis();
  }

  private void saveCurrentScheme(Context context, @Nullable String schemeId, String name) {
    int gestureSet =
        SharedPreferencesUtils.getIntFromStringPref(
            prefs,
            getResources(),
            R.string.pref_gesture_set_key,
            R.string.pref_gesture_set_value_default);
    String scopeValue =
        prefs.getString(
            getString(R.string.pref_gesture_edit_scope_key),
            getString(R.string.pref_gesture_edit_scope_default));
    String scopePackage =
        TextUtils.equals(scopeValue, "current_app") ? currentScopePackage : null;
    JSONObject mappings = collectOverrides(context, gestureSet, scopePackage);
    try {
      JSONObject schemeJson =
          GestureSchemeStore.buildSchemeJson(name, gestureSet, scopeValue, scopePackage, mappings);
      if (schemeId == null) {
        GestureSchemeStore.saveScheme(context, schemeJson);
      } else {
        GestureSchemeStore.saveScheme(context, schemeId, schemeJson);
      }
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_gesture_scheme_saved), context);
      populateSchemeList(context);
    } catch (JSONException e) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_gesture_scheme_invalid), context);
    }
  }

  private JSONObject collectOverrides(
      Context context, int gestureSet, @Nullable String scopePackage) {
    String[] keys = getResources().getStringArray(R.array.pref_shortcut_keys);
    JSONObject mappings = new JSONObject();
    for (String baseKey : keys) {
      String scopedKey = applyScopeToKey(baseKey, scopePackage);
      String prefKey = GestureShortcutMapping.getPrefKeyWithGestureSet(scopedKey, gestureSet);
      if (!prefs.contains(prefKey)) {
        continue;
      }
      String value = prefs.getString(prefKey, null);
      if (TextUtils.isEmpty(value)) {
        continue;
      }
      try {
        mappings.put(baseKey, value);
      } catch (JSONException e) {
        // skip
      }
    }
    return mappings;
  }

  private void showSchemeActions(Context context, GestureSchemeStore.Scheme scheme) {
    String[] items =
        new String[] {
          getString(R.string.pref_gesture_scheme_action_apply),
          getString(R.string.pref_gesture_scheme_action_update),
          getString(R.string.pref_gesture_scheme_action_rename),
          getString(R.string.pref_gesture_scheme_action_export),
          getString(R.string.pref_gesture_scheme_action_delete)
        };
    A11yAlertDialogWrapper.alertDialogBuilder(context)
        .setTitle(R.string.pref_gesture_scheme_action_title)
        .setItems(
            items,
            (dialog, which) -> {
              switch (which) {
                case 0:
                  applyScheme(context, scheme);
                  break;
                case 1:
                  showSaveDialog(context, scheme.id, scheme.name);
                  break;
                case 2:
                  showRenameDialog(context, scheme);
                  break;
                case 3:
                  exportScheme(context, scheme);
                  break;
                case 4:
                  GestureSchemeStore.deleteScheme(context, scheme.id);
                  PreferencesActivityUtils.announceText(
                      getString(R.string.pref_gesture_scheme_deleted), context);
                  populateSchemeList(context);
                  break;
                default:
                  break;
              }
            })
        .show();
  }

  private void showRenameDialog(Context context, GestureSchemeStore.Scheme scheme) {
    EditText input = new EditText(context);
    input.setInputType(InputType.TYPE_CLASS_TEXT);
    input.setText(scheme.name);
    A11yAlertDialogWrapper.alertDialogBuilder(context)
        .setTitle(R.string.pref_gesture_scheme_action_rename)
        .setView(input)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> {
              String name = input.getText().toString().trim();
              if (TextUtils.isEmpty(name)) {
                PreferencesActivityUtils.announceText(
                    getString(R.string.pref_gesture_scheme_invalid), context);
                return;
              }
              try {
                JSONObject clone = GestureSchemeStore.cloneMappings(scheme.mappings);
                JSONObject json =
                    GestureSchemeStore.buildSchemeJson(
                        name, scheme.gestureSet, scheme.scope, scheme.packageName, clone);
                GestureSchemeStore.saveScheme(context, scheme.id, json);
                populateSchemeList(context);
              } catch (JSONException e) {
                PreferencesActivityUtils.announceText(
                    getString(R.string.pref_gesture_scheme_invalid), context);
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void exportScheme(Context context, GestureSchemeStore.Scheme scheme) {
    try {
      JSONObject json =
          GestureSchemeStore.buildSchemeJson(
              scheme.name, scheme.gestureSet, scheme.scope, scheme.packageName, scheme.mappings);
      Intent share = new Intent(Intent.ACTION_SEND);
      share.setType("application/json");
      share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.pref_gesture_scheme_manager_title));
      share.putExtra(Intent.EXTRA_TEXT, json.toString(2));
      startActivity(
          Intent.createChooser(share, getString(R.string.pref_gesture_scheme_manager_title)));
    } catch (JSONException e) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_gesture_scheme_invalid), context);
    }
  }

  private void showImportDialog(Context context) {
    EditText input = new EditText(context);
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    input.setMinLines(6);
    input.setHint(getString(R.string.pref_gesture_scheme_import_summary));
    A11yAlertDialogWrapper.alertDialogBuilder(context)
        .setTitle(R.string.pref_gesture_scheme_import_title)
        .setView(input)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> importScheme(context, input.getText().toString()))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void importScheme(Context context, String json) {
    if (TextUtils.isEmpty(json)) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_gesture_scheme_invalid), context);
      return;
    }
    try {
      JSONObject obj = new JSONObject(json);
      String name = obj.optString("name", getDefaultSchemeName(context));
      int gestureSet = obj.optInt("gestureSet", 0);
      String scope = obj.optString("scope", "global");
      String packageName = obj.optString("package", null);
      JSONObject mappings = obj.optJSONObject("mappings");
      if (mappings == null) {
        PreferencesActivityUtils.announceText(
            getString(R.string.pref_gesture_scheme_invalid), context);
        return;
      }
      JSONObject schemeJson =
          GestureSchemeStore.buildSchemeJson(name, gestureSet, scope, packageName, mappings);
      GestureSchemeStore.saveScheme(context, schemeJson);
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_gesture_scheme_saved), context);
      populateSchemeList(context);
    } catch (JSONException e) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_gesture_scheme_invalid), context);
    }
  }

  private void applyScheme(Context context, GestureSchemeStore.Scheme scheme) {
    String scopeValue = scheme.scope;
    String targetPackage =
        TextUtils.equals(scopeValue, "current_app")
            ? (TextUtils.isEmpty(currentScopePackage) ? scheme.packageName : currentScopePackage)
            : null;
    if (TextUtils.equals(scopeValue, "current_app") && TextUtils.isEmpty(targetPackage)) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_gesture_scheme_invalid), context);
      return;
    }

    Set<String> allowedKeys = new HashSet<>();
    String[] gesturePrefKeys = getResources().getStringArray(R.array.pref_shortcut_keys);
    for (String key : gesturePrefKeys) {
      allowedKeys.add(key);
    }
    Set<String> allowedActions = new HashSet<>(GestureShortcutMapping.getAllActionKeys(context));
    allowedActions.add(getString(R.string.shortcut_value_unassigned));

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
      String scopedKey = applyScopeToKey(baseKey, targetPackage);
      String prefKey = GestureShortcutMapping.getPrefKeyWithGestureSet(scopedKey, scheme.gestureSet);
      editor.putString(prefKey, action);
    }
    editor.putString(
        getString(R.string.pref_gesture_set_key), String.valueOf(scheme.gestureSet));
    editor.apply();
    PreferencesActivityUtils.announceText(
        getString(R.string.pref_gesture_scheme_applied), context);
  }

  private String applyScopeToKey(String baseKey, @Nullable String scopePackage) {
    if (TextUtils.isEmpty(scopePackage)) {
      return baseKey;
    }
    String prefix = getString(R.string.pref_gesture_scope_pkg_prefix);
    return prefix + scopePackage + ":" + baseKey;
  }
}
