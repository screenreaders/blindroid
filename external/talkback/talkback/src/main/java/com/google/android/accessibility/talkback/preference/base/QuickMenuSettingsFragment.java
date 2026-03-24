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
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.preference.PreferencesActivityUtils;
import com.google.android.accessibility.talkback.quickmenu.QuickMenuPreferences;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Settings for configuring the BlindReader quick menu. */
public class QuickMenuSettingsFragment extends TalkbackBaseFragment
    implements Preference.OnPreferenceChangeListener {

  private PreferenceCategory actionCategory;
  private PreferenceCategory savedAppsCategory;
  private @Nullable String currentPackage;
  private @Nullable CharSequence currentLabel;
  private Preference linkCurrentAppPreference;
  private Preference unlinkCurrentAppPreference;
  private Preference useCurrentAsDefaultPreference;
  private Preference exportQuickMenuPreference;
  private Preference importQuickMenuPreference;
  private Preference clearSavedAppsPreference;

  public QuickMenuSettingsFragment() {
    super(R.xml.empty_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.pref_quick_menu_settings_title);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);

    final Context context = getContext();
    if (context == null) {
      return;
    }

    PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
    setPreferenceScreen(screen);

    Preference reset = new Preference(context);
    reset.setKey(context.getString(R.string.pref_quick_menu_reset_key));
    reset.setTitle(R.string.pref_quick_menu_reset_title);
    reset.setSummary(R.string.pref_quick_menu_reset_summary);
    reset.setOnPreferenceClickListener(
        pref -> {
          QuickMenuPreferences.resetToDefaults(context);
          applyDefaultsToUi(context);
          return true;
        });
    screen.addPreference(reset);

    exportQuickMenuPreference = new Preference(context);
    exportQuickMenuPreference.setKey(context.getString(R.string.pref_quick_menu_export_key));
    exportQuickMenuPreference.setTitle(R.string.pref_quick_menu_export_title);
    exportQuickMenuPreference.setSummary(R.string.pref_quick_menu_export_summary);
    exportQuickMenuPreference.setOnPreferenceClickListener(
        pref -> {
          exportQuickMenu(context);
          return true;
        });
    screen.addPreference(exportQuickMenuPreference);

    importQuickMenuPreference = new Preference(context);
    importQuickMenuPreference.setKey(context.getString(R.string.pref_quick_menu_import_key));
    importQuickMenuPreference.setTitle(R.string.pref_quick_menu_import_title);
    importQuickMenuPreference.setSummary(R.string.pref_quick_menu_import_summary);
    importQuickMenuPreference.setOnPreferenceClickListener(
        pref -> {
          showImportQuickMenuDialog(context);
          return true;
        });
    screen.addPreference(importQuickMenuPreference);

    PreferenceCategory appCategory = new PreferenceCategory(context);
    appCategory.setTitle(R.string.pref_quick_menu_app_category_title);
    appCategory.setKey(context.getString(R.string.pref_quick_menu_app_category_key));
    screen.addPreference(appCategory);

    linkCurrentAppPreference = new Preference(context);
    linkCurrentAppPreference.setKey(context.getString(R.string.pref_quick_menu_link_current_key));
    linkCurrentAppPreference.setTitle(R.string.pref_quick_menu_link_current_title);
    linkCurrentAppPreference.setOnPreferenceClickListener(
        pref -> {
          linkCurrentApp(context);
          return true;
        });
    appCategory.addPreference(linkCurrentAppPreference);

    unlinkCurrentAppPreference = new Preference(context);
    unlinkCurrentAppPreference.setKey(context.getString(R.string.pref_quick_menu_unlink_current_key));
    unlinkCurrentAppPreference.setTitle(R.string.pref_quick_menu_unlink_current_title);
    unlinkCurrentAppPreference.setOnPreferenceClickListener(
        pref -> {
          unlinkCurrentApp(context);
          return true;
        });
    appCategory.addPreference(unlinkCurrentAppPreference);

    useCurrentAsDefaultPreference = new Preference(context);
    useCurrentAsDefaultPreference.setKey(context.getString(R.string.pref_quick_menu_use_current_default_key));
    useCurrentAsDefaultPreference.setTitle(R.string.pref_quick_menu_use_current_default_title);
    useCurrentAsDefaultPreference.setOnPreferenceClickListener(
        pref -> {
          applyCurrentAsDefault(context);
          return true;
        });
    appCategory.addPreference(useCurrentAsDefaultPreference);

    actionCategory = new PreferenceCategory(context);
    actionCategory.setTitle(R.string.pref_quick_menu_actions_title);
    actionCategory.setKey(context.getString(R.string.pref_quick_menu_actions_category_key));
    screen.addPreference(actionCategory);

    savedAppsCategory = new PreferenceCategory(context);
    savedAppsCategory.setTitle(R.string.pref_quick_menu_saved_apps_title);
    savedAppsCategory.setKey(context.getString(R.string.pref_quick_menu_saved_apps_category_key));
    screen.addPreference(savedAppsCategory);

    clearSavedAppsPreference = new Preference(context);
    clearSavedAppsPreference.setKey(context.getString(R.string.pref_quick_menu_clear_saved_key));
    clearSavedAppsPreference.setTitle(R.string.pref_quick_menu_clear_saved_title);
    clearSavedAppsPreference.setSummary(R.string.pref_quick_menu_clear_saved_summary);
    clearSavedAppsPreference.setOnPreferenceClickListener(
        pref -> {
          clearAllSavedMenus(context);
          return true;
        });
    savedAppsCategory.addPreference(clearSavedAppsPreference);

    buildActionList(context);
    updateAppLinkUi(context);
    populateSavedApps(context);
  }

  @Override
  public void onResume() {
    super.onResume();
    Context context = getContext();
    if (context == null) {
      return;
    }
    updateAppLinkUi(context);
    populateSavedApps(context);
  }

  private void buildActionList(Context context) {
    String unassigned = context.getString(R.string.shortcut_value_unassigned);
    List<String> actions = QuickMenuPreferences.getSupportedActions(context);
    for (String actionKey : actions) {
      if (TextUtils.equals(actionKey, unassigned)) {
        continue;
      }
      CheckBoxPreference pref = new CheckBoxPreference(context);
      pref.setKey(QuickMenuPreferences.buildActionPrefKey(context, actionKey));
      pref.setTitle(QuickMenuPreferences.getActionLabel(context, actionKey));
      pref.setChecked(QuickMenuPreferences.isActionEnabled(context, actionKey));
      pref.setOnPreferenceChangeListener(this);
      actionCategory.addPreference(pref);
    }
  }

  private void applyDefaultsToUi(Context context) {
    if (actionCategory == null) {
      return;
    }
    int count = actionCategory.getPreferenceCount();
    for (int i = 0; i < count; i++) {
      Preference preference = actionCategory.getPreference(i);
      if (!(preference instanceof CheckBoxPreference)) {
        continue;
      }
      String key = preference.getKey();
      if (key == null) {
        continue;
      }
      if (!key.startsWith(context.getString(R.string.pref_quick_menu_action_prefix))) {
        continue;
      }
      String actionKey = key.substring(context.getString(R.string.pref_quick_menu_action_prefix).length());
      ((CheckBoxPreference) preference).setChecked(
          QuickMenuPreferences.isActionEnabled(context, actionKey));
    }
  }

  private void updateAppLinkUi(Context context) {
    resolveCurrentApp(context);
    boolean hasApp = !TextUtils.isEmpty(currentPackage);
    boolean linked = hasApp && QuickMenuPreferences.hasActionsForPackage(context, currentPackage);

    if (linkCurrentAppPreference != null) {
      if (hasApp) {
        linkCurrentAppPreference.setSummary(
            getString(R.string.pref_quick_menu_link_current_summary, currentLabel));
      } else {
        linkCurrentAppPreference.setSummary(R.string.pref_quick_menu_link_current_missing);
      }
      linkCurrentAppPreference.setEnabled(hasApp);
    }
    if (unlinkCurrentAppPreference != null) {
      if (linked) {
        unlinkCurrentAppPreference.setSummary(
            getString(R.string.pref_quick_menu_unlink_current_summary, currentLabel));
      } else if (hasApp) {
        unlinkCurrentAppPreference.setSummary(R.string.pref_quick_menu_unlink_current_missing);
      } else {
        unlinkCurrentAppPreference.setSummary(R.string.pref_quick_menu_link_current_missing);
      }
      unlinkCurrentAppPreference.setEnabled(hasApp && linked);
    }
    if (useCurrentAsDefaultPreference != null) {
      if (linked) {
        useCurrentAsDefaultPreference.setSummary(
            getString(R.string.pref_quick_menu_use_current_default_summary, currentLabel));
      } else if (hasApp) {
        useCurrentAsDefaultPreference.setSummary(R.string.pref_quick_menu_use_current_default_missing);
      } else {
        useCurrentAsDefaultPreference.setSummary(R.string.pref_quick_menu_link_current_missing);
      }
      useCurrentAsDefaultPreference.setEnabled(hasApp && linked);
    }
  }

  private void populateSavedApps(Context context) {
    if (savedAppsCategory == null) {
      return;
    }
    savedAppsCategory.removeAll();
    if (clearSavedAppsPreference != null) {
      savedAppsCategory.addPreference(clearSavedAppsPreference);
    }
    List<String> packages = QuickMenuPreferences.getLinkedPackages(context);
    if (packages.isEmpty()) {
      Preference empty = new Preference(context);
      empty.setSelectable(false);
      empty.setTitle(R.string.pref_quick_menu_saved_apps_empty);
      savedAppsCategory.addPreference(empty);
      return;
    }
    for (String packageName : packages) {
      CharSequence label = packageName;
      try {
        ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, 0);
        label = context.getPackageManager().getApplicationLabel(info);
      } catch (PackageManager.NameNotFoundException e) {
        label = packageName;
      }
      Preference pref = new Preference(context);
      pref.setTitle(label);
      List<String> actions = QuickMenuPreferences.getActionsForPackage(context, packageName);
      int count = actions == null ? 0 : actions.size();
      String actionSummary = buildActionsSummary(context, actions);
      pref.setSummary(
          getString(R.string.pref_quick_menu_saved_apps_summary, count)
              + (actionSummary.isEmpty() ? "" : " • " + actionSummary));
      pref.setOnPreferenceClickListener(
          clicked -> {
            showEditDialog(context, packageName, label);
            return true;
          });
      savedAppsCategory.addPreference(pref);
    }
  }

  private void exportQuickMenu(Context context) {
    try {
      JSONObject payload = new JSONObject();
      payload.put("version", 1);
      JSONArray array = new JSONArray();
      for (String action : QuickMenuPreferences.getGlobalActions(context)) {
        array.put(action);
      }
      payload.put("actions", array);
      Intent share = new Intent(Intent.ACTION_SEND);
      share.setType("application/json");
      share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.pref_quick_menu_export_title));
      share.putExtra(Intent.EXTRA_TEXT, payload.toString(2));
      startActivity(Intent.createChooser(share, getString(R.string.pref_quick_menu_export_title)));
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_quick_menu_export_success), context);
    } catch (JSONException e) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_quick_menu_export_failed), context);
    }
  }

  private void showImportQuickMenuDialog(Context context) {
    android.widget.EditText input = new android.widget.EditText(context);
    input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    input.setMinLines(6);
    input.setHint(getString(R.string.pref_quick_menu_import_summary));
    A11yAlertDialogWrapper.alertDialogBuilder(context)
        .setTitle(R.string.pref_quick_menu_import_title)
        .setView(input)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> importQuickMenu(context, input.getText().toString()))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void importQuickMenu(Context context, String json) {
    if (TextUtils.isEmpty(json)) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_quick_menu_import_failed), context);
      return;
    }
    JSONObject root;
    try {
      root = new JSONObject(json);
    } catch (JSONException e) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_quick_menu_import_failed), context);
      return;
    }
    JSONArray actionsJson = root.optJSONArray("actions");
    if (actionsJson == null) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_quick_menu_import_failed), context);
      return;
    }
    List<String> actions = new ArrayList<>();
    for (int i = 0; i < actionsJson.length(); i++) {
      String value = actionsJson.optString(i, null);
      if (!TextUtils.isEmpty(value)) {
        actions.add(value);
      }
    }
    QuickMenuPreferences.setGlobalActions(context, actions);
    PreferencesActivityUtils.announceText(
        getString(R.string.pref_quick_menu_import_success), context);
  }

  private void showEditDialog(Context context, String packageName, CharSequence label) {
    List<String> supported = QuickMenuPreferences.getSupportedActions(context);
    String unassigned = context.getString(R.string.shortcut_value_unassigned);
    List<String> filtered = new ArrayList<>();
    for (String action : supported) {
      if (!TextUtils.equals(action, unassigned)) {
        filtered.add(action);
      }
    }
    List<String> existing = QuickMenuPreferences.getActionsForPackage(context, packageName);
    boolean[] checked = new boolean[filtered.size()];
    CharSequence[] titles = new CharSequence[filtered.size()];
    for (int i = 0; i < filtered.size(); i++) {
      String actionKey = filtered.get(i);
      titles[i] = QuickMenuPreferences.getActionLabel(context, actionKey);
      checked[i] = existing != null && existing.contains(actionKey);
    }
    A11yAlertDialogWrapper.alertDialogBuilder(context)
        .setTitle(getString(R.string.pref_quick_menu_saved_apps_edit_title, label))
        .setMultiChoiceItems(
            titles,
            checked,
            (dialog, which, isChecked) -> checked[which] = isChecked)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> {
              List<String> actions = new ArrayList<>();
              for (int i = 0; i < filtered.size(); i++) {
                if (checked[i]) {
                  actions.add(filtered.get(i));
                }
              }
              QuickMenuPreferences.saveActionsForPackage(context, packageName, actions);
              PreferencesActivityUtils.announceText(
                  getString(R.string.pref_quick_menu_saved_apps_updated, label), context);
              populateSavedApps(context);
              updateAppLinkUi(context);
            })
        .setNeutralButton(
            R.string.pref_quick_menu_saved_apps_remove_action,
            (dialog, which) -> {
              QuickMenuPreferences.removeActionsForPackage(context, packageName);
              PreferencesActivityUtils.announceText(
                  getString(R.string.pref_quick_menu_saved_apps_removed, label), context);
              populateSavedApps(context);
              updateAppLinkUi(context);
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void clearAllSavedMenus(Context context) {
    List<String> packages = QuickMenuPreferences.getLinkedPackages(context);
    for (String packageName : packages) {
      QuickMenuPreferences.removeActionsForPackage(context, packageName);
    }
    PreferencesActivityUtils.announceText(
        getString(R.string.pref_quick_menu_clear_saved_announce), context);
    populateSavedApps(context);
    updateAppLinkUi(context);
  }

  private String buildActionsSummary(Context context, @Nullable List<String> actions) {
    if (actions == null || actions.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    int maxLen = 80;
    for (String action : actions) {
      String label = QuickMenuPreferences.getActionLabel(context, action);
      if (TextUtils.isEmpty(label)) {
        continue;
      }
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(label);
      if (sb.length() > maxLen) {
        sb.append("…");
        break;
      }
    }
    return sb.toString();
  }

  private void resolveCurrentApp(Context context) {
    TalkBackService service = TalkBackService.getInstance();
    if (service == null || !TalkBackService.isServiceActive()) {
      currentPackage = null;
      currentLabel = null;
      return;
    }
    if (service.getRootInActiveWindow() == null
        || service.getRootInActiveWindow().getPackageName() == null) {
      currentPackage = null;
      currentLabel = null;
      return;
    }
    currentPackage = service.getRootInActiveWindow().getPackageName().toString();
    PackageManager pm = context.getPackageManager();
    try {
      ApplicationInfo info = pm.getApplicationInfo(currentPackage, 0);
      currentLabel = pm.getApplicationLabel(info);
    } catch (PackageManager.NameNotFoundException e) {
      currentLabel = currentPackage;
    }
  }

  private void linkCurrentApp(Context context) {
    resolveCurrentApp(context);
    if (TextUtils.isEmpty(currentPackage)) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_quick_menu_link_current_missing), context);
      return;
    }
    List<String> actions = collectSelectedActions();
    QuickMenuPreferences.saveActionsForPackage(context, currentPackage, actions);
    PreferencesActivityUtils.announceText(
        getString(R.string.pref_quick_menu_link_current_announce, currentLabel), context);
    updateAppLinkUi(context);
  }

  private void unlinkCurrentApp(Context context) {
    resolveCurrentApp(context);
    if (TextUtils.isEmpty(currentPackage)) {
      return;
    }
    QuickMenuPreferences.removeActionsForPackage(context, currentPackage);
    PreferencesActivityUtils.announceText(
        getString(R.string.pref_quick_menu_unlink_current_announce, currentLabel), context);
    updateAppLinkUi(context);
  }

  private void applyCurrentAsDefault(Context context) {
    resolveCurrentApp(context);
    if (TextUtils.isEmpty(currentPackage)) {
      return;
    }
    List<String> actions = QuickMenuPreferences.getActionsForPackage(context, currentPackage);
    if (actions == null || actions.isEmpty()) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_quick_menu_use_current_default_missing), context);
      return;
    }
    QuickMenuPreferences.setGlobalActions(context, actions);
    PreferencesActivityUtils.announceText(
        getString(R.string.pref_quick_menu_use_current_default_announce, currentLabel), context);
  }

  private List<String> collectSelectedActions() {
    List<String> actions = new ArrayList<>();
    if (actionCategory == null || getContext() == null) {
      return actions;
    }
    String prefix = getContext().getString(R.string.pref_quick_menu_action_prefix);
    int count = actionCategory.getPreferenceCount();
    for (int i = 0; i < count; i++) {
      Preference preference = actionCategory.getPreference(i);
      if (!(preference instanceof CheckBoxPreference)) {
        continue;
      }
      if (!((CheckBoxPreference) preference).isChecked()) {
        continue;
      }
      String key = preference.getKey();
      if (key == null || !key.startsWith(prefix)) {
        continue;
      }
      actions.add(key.substring(prefix.length()));
    }
    return actions;
  }

  @Override
  public boolean onPreferenceChange(Preference preference, Object newValue) {
    Context context = getContext();
    if (context == null) {
      return true;
    }
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    boolean wasCustomized = QuickMenuPreferences.isCustomized(context, prefs);
    if (!wasCustomized) {
      persistAllActionStates(prefs, preference.getKey(), (Boolean) newValue);
    }
    QuickMenuPreferences.markCustomized(context);
    return true;
  }

  private void persistAllActionStates(
      SharedPreferences prefs, String changedKey, boolean changedValue) {
    if (actionCategory == null) {
      return;
    }
    SharedPreferences.Editor editor = prefs.edit();
    int count = actionCategory.getPreferenceCount();
    for (int i = 0; i < count; i++) {
      Preference preference = actionCategory.getPreference(i);
      if (!(preference instanceof CheckBoxPreference)) {
        continue;
      }
      String key = preference.getKey();
      if (key == null) {
        continue;
      }
      boolean value =
          key.equals(changedKey) ? changedValue : ((CheckBoxPreference) preference).isChecked();
      editor.putBoolean(key, value);
    }
    editor.apply();
  }
}
