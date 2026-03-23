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

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.preference.Preference;
import androidx.preference.EditTextPreference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.gesture.GestureMacroStore;

/** Preferences for gesture macros. */
public class GestureMacroSettingsFragment extends TalkbackBaseFragment {

  private Preference macro1ActionsPreference;
  private Preference macro2ActionsPreference;
  private Preference macro3ActionsPreference;

  public GestureMacroSettingsFragment() {
    super(R.xml.gesture_macro_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.pref_gesture_macro_settings_title);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    setupNameSummary(R.string.pref_macro_1_name_key, R.string.macro_default_name_1);
    setupNameSummary(R.string.pref_macro_2_name_key, R.string.macro_default_name_2);
    setupNameSummary(R.string.pref_macro_3_name_key, R.string.macro_default_name_3);
    macro1ActionsPreference =
        setupActionsPreference(R.string.pref_macro_1_actions_list_key, /* macroIndex= */ 1);
    macro2ActionsPreference =
        setupActionsPreference(R.string.pref_macro_2_actions_list_key, /* macroIndex= */ 2);
    macro3ActionsPreference =
        setupActionsPreference(R.string.pref_macro_3_actions_list_key, /* macroIndex= */ 3);
  }

  @Override
  public void onResume() {
    super.onResume();
    updateActionsSummary(macro1ActionsPreference, 1);
    updateActionsSummary(macro2ActionsPreference, 2);
    updateActionsSummary(macro3ActionsPreference, 3);
  }

  private void setupNameSummary(int keyResId, int defaultNameResId) {
    EditTextPreference preference = (EditTextPreference) findPreference(getString(keyResId));
    if (preference == null) {
      return;
    }
    preference.setSummaryProvider(
        pref -> {
          String value = preference.getText();
          if (TextUtils.isEmpty(value)) {
            return getString(defaultNameResId);
          }
          return value;
        });
  }

  private Preference setupActionsPreference(int keyResId, int macroIndex) {
    Preference preference = findPreference(getString(keyResId));
    if (preference == null) {
      return null;
    }
    updateActionsSummary(preference, macroIndex);
    preference.setOnPreferenceClickListener(
        pref -> {
          Intent intent = new Intent(getContext(), GestureMacroEditorActivity.class);
          intent.putExtra(GestureMacroEditorActivity.EXTRA_MACRO_INDEX, macroIndex);
          startActivity(intent);
          return true;
        });
    return preference;
  }

  private void updateActionsSummary(Preference preference, int macroIndex) {
    if (preference == null || getContext() == null) {
      return;
    }
    int count = GestureMacroStore.getActionList(getContext(), macroIndex).size();
    preference.setSummary(getString(R.string.pref_macro_actions_summary, count));
  }
}
