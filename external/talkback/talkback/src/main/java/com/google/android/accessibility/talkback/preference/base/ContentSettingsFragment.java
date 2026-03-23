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

import android.os.Bundle;
import androidx.preference.ListPreference;
import com.google.android.accessibility.talkback.R;

/** Preferences for content reading filters and OCR auto-read. */
public class ContentSettingsFragment extends TalkbackBaseFragment {

  public ContentSettingsFragment() {
    super(R.xml.content_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.pref_content_settings_title);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    setupListSummary(R.string.pref_read_all_filter_key);
    setupListSummary(R.string.pref_read_all_style_key);
  }

  private void setupListSummary(int keyResId) {
    ListPreference preference = (ListPreference) findPreference(getString(keyResId));
    if (preference == null) {
      return;
    }
    preference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
  }
}
