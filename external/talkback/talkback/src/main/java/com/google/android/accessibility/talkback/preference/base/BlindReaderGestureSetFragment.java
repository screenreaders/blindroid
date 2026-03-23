/*
 * Copyright 2026
 */
package com.google.android.accessibility.talkback.preference.base;

import android.content.Context;
import android.os.Bundle;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/**
 * Lightweight screen for switching BlindReader gesture set.
 */
public class BlindReaderGestureSetFragment extends TalkbackBaseFragment {

  public BlindReaderGestureSetFragment() {
    super(R.xml.gesture_set_quick_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.pref_blindreader_gesture_set_title);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    Context context = getContext();
    if (context == null) {
      return;
    }
    Preference pref = findPreference(context.getString(R.string.pref_gesture_set_key));
    if (pref instanceof ListPreference) {
      ListPreference listPreference = (ListPreference) pref;
      int current =
          SharedPreferencesUtils.getIntFromStringPref(
              SharedPreferencesUtils.getSharedPreferences(context),
              context.getResources(),
              R.string.pref_gesture_set_key,
              R.string.pref_gesture_set_value_default);
      CharSequence[] entries = listPreference.getEntries();
      if (entries != null && current >= 0 && current < entries.length) {
        listPreference.setSummary(entries[current]);
      }
    }
  }
}
