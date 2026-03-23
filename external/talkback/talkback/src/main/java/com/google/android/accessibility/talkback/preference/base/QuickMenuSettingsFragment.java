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
import android.os.Bundle;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.quickmenu.QuickMenuPreferences;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.List;

/** Settings for configuring the BlindReader quick menu. */
public class QuickMenuSettingsFragment extends TalkbackBaseFragment
    implements Preference.OnPreferenceChangeListener {

  private PreferenceCategory actionCategory;

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

    actionCategory = new PreferenceCategory(context);
    actionCategory.setTitle(R.string.pref_quick_menu_actions_title);
    actionCategory.setKey(context.getString(R.string.pref_quick_menu_actions_category_key));
    screen.addPreference(actionCategory);

    buildActionList(context);
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
