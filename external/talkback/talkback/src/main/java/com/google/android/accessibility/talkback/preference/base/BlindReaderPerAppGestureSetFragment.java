/*
 * Copyright 2026
 */
package com.google.android.accessibility.talkback.preference.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.gesture.PerAppGestureSetUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Preference screen for per-app gesture set overrides. */
public class BlindReaderPerAppGestureSetFragment extends TalkbackBaseFragment {

  private @Nullable String currentPackageName;
  private @Nullable CharSequence currentAppLabel;

  public BlindReaderPerAppGestureSetFragment() {
    super(R.xml.per_app_gesture_set_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.pref_per_app_gesture_set_title);
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
    populateSavedOverrides(context);
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
    populateSavedOverrides(context);
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
        findPreference(context.getString(R.string.pref_per_app_gesture_set_enabled_key));
    ListPreference currentAppPref =
        findPreference(context.getString(R.string.pref_per_app_gesture_set_current_key));
    Preference clearPref =
        findPreference(context.getString(R.string.pref_per_app_gesture_set_clear_key));

    if (currentAppPref == null || clearPref == null || enablePref == null) {
      return;
    }

    if (TextUtils.isEmpty(currentPackageName)) {
      currentAppPref.setEnabled(false);
      clearPref.setEnabled(false);
      currentAppPref.setSummary(R.string.pref_per_app_gesture_set_current_unavailable);
      return;
    }

    int currentSet =
        PerAppGestureSetUtils.getGestureSetForPackage(prefs, context, currentPackageName);
    currentAppPref.setValue(String.valueOf(currentSet));
    updateCurrentAppSummary(currentAppPref, currentSet);

    currentAppPref.setOnPreferenceChangeListener(
        (preference, newValue) -> {
          int set = parseGestureSetValue(newValue);
          PerAppGestureSetUtils.setGestureSetForPackage(
              prefs, context, currentPackageName, set);
          updateCurrentAppSummary(currentAppPref, set);
          return true;
        });

    clearPref.setOnPreferenceClickListener(
        preference -> {
          PerAppGestureSetUtils.setGestureSetForPackage(
              prefs, context, currentPackageName, PerAppGestureSetUtils.SET_GLOBAL);
          currentAppPref.setValue(String.valueOf(PerAppGestureSetUtils.SET_GLOBAL));
          updateCurrentAppSummary(currentAppPref, PerAppGestureSetUtils.SET_GLOBAL);
          return true;
        });
  }

  private void populateSavedOverrides(Context context) {
    PreferenceCategory listCategory =
        findPreference(context.getString(R.string.pref_per_app_gesture_set_list_key));
    if (listCategory == null) {
      return;
    }
    listCategory.removeAll();

    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String prefix = context.getString(R.string.pref_per_app_gesture_set_prefix);
    List<String> packageNames = new ArrayList<>();
    for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
      if (entry.getKey().startsWith(prefix)) {
        packageNames.add(entry.getKey().substring(prefix.length()));
      }
    }

    if (packageNames.isEmpty()) {
      Preference emptyPref = new Preference(context);
      emptyPref.setTitle(R.string.pref_per_app_gesture_set_list_empty);
      emptyPref.setSelectable(false);
      listCategory.addPreference(emptyPref);
      return;
    }

    Collections.sort(packageNames);
    PackageManager pm = context.getPackageManager();
    for (String packageName : packageNames) {
      String prefKey = PerAppGestureSetUtils.getPrefKey(context, packageName);
      ListPreference pref = new ListPreference(context);
      pref.setKey(prefKey);
      pref.setEntries(R.array.pref_per_app_gesture_set_entries);
      pref.setEntryValues(R.array.pref_per_app_gesture_set_values);
      pref.setDefaultValue(String.valueOf(PerAppGestureSetUtils.SET_GLOBAL));

      CharSequence label = packageName;
      try {
        ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
        CharSequence appLabel = pm.getApplicationLabel(info);
        if (!TextUtils.isEmpty(appLabel)) {
          label = appLabel;
        }
      } catch (PackageManager.NameNotFoundException e) {
        // Use package name.
      }

      pref.setTitle(label);
      int currentSet =
          PerAppGestureSetUtils.getGestureSetForPackage(prefs, context, packageName);
      pref.setValue(String.valueOf(currentSet));
      updateSavedOverrideSummary(pref, currentSet);
      pref.setOnPreferenceChangeListener(
          (preference, newValue) -> {
            int set = parseGestureSetValue(newValue);
            PerAppGestureSetUtils.setGestureSetForPackage(prefs, context, packageName, set);
            updateSavedOverrideSummary(pref, set);
            return true;
          });
      listCategory.addPreference(pref);
    }
  }

  private void updateSavedOverrideSummary(ListPreference pref, int gestureSet) {
    CharSequence entry = findEntryLabel(pref, gestureSet);
    if (TextUtils.isEmpty(entry)) {
      pref.setSummary(R.string.pref_per_app_gesture_set_entry_global);
    } else {
      pref.setSummary(entry);
    }
  }

  private int parseGestureSetValue(Object newValue) {
    if (newValue == null) {
      return PerAppGestureSetUtils.SET_GLOBAL;
    }
    try {
      return Integer.parseInt(newValue.toString());
    } catch (NumberFormatException e) {
      return PerAppGestureSetUtils.SET_GLOBAL;
    }
  }

  private void updateCurrentAppSummary(ListPreference pref, int gestureSet) {
    CharSequence appLabel = TextUtils.isEmpty(currentAppLabel) ? currentPackageName : currentAppLabel;
    CharSequence entryLabel = findEntryLabel(pref, gestureSet);
    if (TextUtils.isEmpty(appLabel)) {
      pref.setSummary(R.string.pref_per_app_gesture_set_current_unavailable);
      return;
    }
    if (TextUtils.isEmpty(entryLabel)) {
      pref.setSummary(
          getString(R.string.pref_per_app_gesture_set_current_summary, appLabel));
    } else {
      pref.setSummary(
          getString(R.string.pref_per_app_gesture_set_current_summary_with_value, appLabel, entryLabel));
    }
  }

  private @Nullable CharSequence findEntryLabel(ListPreference pref, int value) {
    String valueString = String.valueOf(value);
    CharSequence[] entryValues = pref.getEntryValues();
    CharSequence[] entries = pref.getEntries();
    if (entryValues == null || entries == null) {
      return null;
    }
    for (int i = 0; i < entryValues.length && i < entries.length; i++) {
      if (TextUtils.equals(entryValues[i], valueString)) {
        return entries[i];
      }
    }
    return null;
  }
}
