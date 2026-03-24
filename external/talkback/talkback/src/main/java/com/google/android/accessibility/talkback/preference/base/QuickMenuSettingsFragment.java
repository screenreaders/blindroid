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
import android.os.Bundle;
import android.text.TextUtils;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.preference.PreferencesActivityUtils;
import com.google.android.accessibility.talkback.quickmenu.QuickMenuPreferences;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Settings for configuring the BlindReader quick menu. */
public class QuickMenuSettingsFragment extends TalkbackBaseFragment
    implements Preference.OnPreferenceChangeListener {

  private PreferenceCategory actionCategory;
  private @Nullable String currentPackage;
  private @Nullable CharSequence currentLabel;
  private Preference linkCurrentAppPreference;
  private Preference unlinkCurrentAppPreference;

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

    actionCategory = new PreferenceCategory(context);
    actionCategory.setTitle(R.string.pref_quick_menu_actions_title);
    actionCategory.setKey(context.getString(R.string.pref_quick_menu_actions_category_key));
    screen.addPreference(actionCategory);

    buildActionList(context);
    updateAppLinkUi(context);
  }

  @Override
  public void onResume() {
    super.onResume();
    Context context = getContext();
    if (context == null) {
      return;
    }
    updateAppLinkUi(context);
  }

  private void buildActionList(Context context) {
    List<String> actions = QuickMenuPreferences.getSupportedActions(context);
    for (String actionKey : actions) {
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
