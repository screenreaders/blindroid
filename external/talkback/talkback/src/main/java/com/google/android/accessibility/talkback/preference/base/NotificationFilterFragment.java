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
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.notification.NotificationFilterUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Preference screen for filtering notification speech per app. */
public class NotificationFilterFragment extends TalkbackBaseFragment {

  private @Nullable String currentPackageName;
  private @Nullable CharSequence currentAppLabel;

  public NotificationFilterFragment() {
    super(R.xml.notification_filter_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.pref_notification_filter_title);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    Context context = getContext();
    if (context == null) {
      return;
    }
    resolveCurrentApp(context);
    setupPreferences(context);
    populateMutedList(context);
  }

  @Override
  public void onResume() {
    super.onResume();
    Context context = getContext();
    if (context == null) {
      return;
    }
    resolveCurrentApp(context);
    setupPreferences(context);
    populateMutedList(context);
  }

  private void resolveCurrentApp(Context context) {
    TalkBackService service = TalkBackService.getInstance();
    if (service == null || !TalkBackService.isServiceActive()) {
      currentPackageName = null;
      currentAppLabel = null;
      return;
    }
    if (service.getRootInActiveWindow() == null
        || service.getRootInActiveWindow().getPackageName() == null) {
      currentPackageName = null;
      currentAppLabel = null;
      return;
    }
    currentPackageName = service.getRootInActiveWindow().getPackageName().toString();
    PackageManager pm = context.getPackageManager();
    try {
      ApplicationInfo info = pm.getApplicationInfo(currentPackageName, 0);
      currentAppLabel = pm.getApplicationLabel(info);
    } catch (PackageManager.NameNotFoundException e) {
      currentAppLabel = currentPackageName;
    }
  }

  private void setupPreferences(Context context) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    SwitchPreferenceCompat enablePref =
        findPreference(context.getString(R.string.pref_notification_filter_enabled_key));
    SwitchPreferenceCompat currentPref =
        findPreference(context.getString(R.string.pref_notification_filter_current_key));

    if (enablePref == null || currentPref == null) {
      return;
    }

    boolean enabled = NotificationFilterUtils.isFilterEnabled(context, prefs);
    enablePref.setOnPreferenceChangeListener(
        (preference, newValue) -> {
          populateMutedList(context);
          updateCurrentAppPref(context, currentPref, (Boolean) newValue);
          return true;
        });

    updateCurrentAppPref(context, currentPref, enabled);
  }

  private void updateCurrentAppPref(
      Context context, SwitchPreferenceCompat currentPref, boolean enabled) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    if (!enabled) {
      currentPref.setEnabled(false);
      currentPref.setSummary(R.string.pref_notification_filter_disabled_summary);
      return;
    }
    if (TextUtils.isEmpty(currentPackageName)) {
      currentPref.setEnabled(false);
      currentPref.setSummary(R.string.pref_notification_filter_current_unavailable);
      return;
    }
    CharSequence appLabel =
        TextUtils.isEmpty(currentAppLabel) ? currentPackageName : currentAppLabel;
    currentPref.setEnabled(true);
    currentPref.setTitle(getString(R.string.pref_notification_filter_current_title, appLabel));
    boolean muted = NotificationFilterUtils.isPackageMuted(prefs, context, currentPackageName);
    currentPref.setChecked(muted);
    currentPref.setSummary(
        muted
            ? R.string.pref_notification_filter_current_summary_on
            : R.string.pref_notification_filter_current_summary_off);
    currentPref.setOnPreferenceChangeListener(
        (preference, newValue) -> {
          NotificationFilterUtils.setPackageMuted(
              prefs, context, currentPackageName, (Boolean) newValue);
          populateMutedList(context);
          return true;
        });
  }

  private void populateMutedList(Context context) {
    PreferenceCategory listCategory =
        findPreference(context.getString(R.string.pref_notification_filter_list_key));
    if (listCategory == null) {
      return;
    }
    listCategory.removeAll();

    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    boolean enabled = NotificationFilterUtils.isFilterEnabled(context, prefs);
    List<String> packageNames = NotificationFilterUtils.getMutedPackages(prefs, context);

    if (!enabled || packageNames.isEmpty()) {
      Preference emptyPref = new Preference(context);
      emptyPref.setTitle(
          enabled
              ? R.string.pref_notification_filter_list_empty
              : R.string.pref_notification_filter_disabled_summary);
      emptyPref.setSelectable(false);
      listCategory.addPreference(emptyPref);
      return;
    }

    PackageManager pm = context.getPackageManager();
    Collections.sort(
        packageNames,
        Comparator.comparing(
            pkg -> {
              try {
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                CharSequence label = pm.getApplicationLabel(info);
                return label == null ? pkg : label.toString();
              } catch (PackageManager.NameNotFoundException e) {
                return pkg;
              }
            },
            String.CASE_INSENSITIVE_ORDER));

    for (String packageName : packageNames) {
      SwitchPreferenceCompat pref = new SwitchPreferenceCompat(context);
      pref.setKey(NotificationFilterUtils.getPrefKey(context, packageName));
      pref.setChecked(true);
      CharSequence label = packageName;
      try {
        ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
        CharSequence appLabel = pm.getApplicationLabel(info);
        if (!TextUtils.isEmpty(appLabel)) {
          label = appLabel;
        }
      } catch (PackageManager.NameNotFoundException e) {
        // Keep package name.
      }
      pref.setTitle(label);
      pref.setSummary(R.string.pref_notification_filter_list_item_summary);
      pref.setOnPreferenceChangeListener(
          (preference, newValue) -> {
            boolean muted = (Boolean) newValue;
            NotificationFilterUtils.setPackageMuted(prefs, context, packageName, muted);
            populateMutedList(context);
            return true;
          });
      listCategory.addPreference(pref);
    }
  }
}
