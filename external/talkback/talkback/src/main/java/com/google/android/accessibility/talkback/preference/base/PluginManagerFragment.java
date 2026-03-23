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
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.plugin.PluginContract;
import java.util.List;

/** Lists BlindReader plugins and opens their settings activities. */
public class PluginManagerFragment extends TalkbackBaseFragment {

  public PluginManagerFragment() {
    super(R.xml.empty_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.pref_plugins_settings_title);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);

    Context context = getContext();
    if (context == null) {
      return;
    }

    PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
    setPreferenceScreen(screen);

    PackageManager pm = context.getPackageManager();
    Intent queryIntent = new Intent(PluginContract.ACTION_PLUGIN_SETTINGS);
    List<ResolveInfo> plugins = pm.queryIntentActivities(queryIntent, 0);
    if (plugins == null || plugins.isEmpty()) {
      Preference empty = new Preference(context);
      empty.setSelectable(false);
      empty.setTitle(R.string.plugins_empty);
      screen.addPreference(empty);
      return;
    }

    for (ResolveInfo info : plugins) {
      Preference pref = new Preference(context);
      pref.setTitle(info.loadLabel(pm));
      if (info.activityInfo != null) {
        pref.setSummary(info.activityInfo.packageName);
        Intent launch = new Intent();
        launch.setClassName(
            info.activityInfo.packageName, info.activityInfo.name);
        pref.setIntent(launch);
      }
      screen.addPreference(pref);
    }
  }
}
