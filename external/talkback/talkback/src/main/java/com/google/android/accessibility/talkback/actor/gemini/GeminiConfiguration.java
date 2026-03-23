/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.android.accessibility.talkback.actor.gemini;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/** This class implements the configuration flags for Gemini requests. */
public final class GeminiConfiguration {

  private GeminiConfiguration() {}

  public static boolean isGeminiVoiceCommandEnabled(Context context) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    return SharedPreferencesUtils.getBooleanPref(
        prefs, context.getResources(), R.string.pref_gemini_enabled_key, R.bool.pref_gemini_opt_in_default);
  }

  static String getGeminiModel(Context context) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String value =
        SharedPreferencesUtils.getStringPref(
            prefs,
            context.getResources(),
            R.string.pref_gemini_model_key,
            R.string.pref_gemini_model_default);
    if (TextUtils.isEmpty(value)) {
      return context.getString(R.string.pref_gemini_model_default);
    }
    return value;
  }

  static String getPrefixPrompt(Context context) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String value =
        SharedPreferencesUtils.getStringPref(
            prefs,
            context.getResources(),
            R.string.pref_gemini_prefix_prompt_key,
            R.string.pref_gemini_prefix_prompt_default);
    return value == null ? "" : value;
  }

  static String getReadScreenPrompt(Context context) {
    String style = getReadScreenStyle(context);
    if (TextUtils.equals(
        style, context.getString(R.string.pref_gemini_read_screen_style_value_summary))) {
      return context.getString(R.string.gemini_read_screen_summary_prompt);
    }
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String value =
        SharedPreferencesUtils.getStringPref(
            prefs,
            context.getResources(),
            R.string.pref_gemini_read_screen_prompt_key,
            R.string.gemini_read_screen_default_prompt);
    if (TextUtils.isEmpty(value)) {
      return context.getString(R.string.gemini_read_screen_default_prompt);
    }
    return value;
  }

  static String getReadScreenStyle(Context context) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String value =
        SharedPreferencesUtils.getStringPref(
            prefs,
            context.getResources(),
            R.string.pref_gemini_read_screen_style_key,
            R.string.pref_gemini_read_screen_style_default);
    if (TextUtils.isEmpty(value)) {
      return context.getString(R.string.pref_gemini_read_screen_style_default);
    }
    return value;
  }

  static boolean isReadScreenHapticEnabled(Context context) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    return SharedPreferencesUtils.getBooleanPref(
        prefs,
        context.getResources(),
        R.string.pref_gemini_read_screen_haptic_key,
        R.bool.pref_gemini_read_screen_haptic_default);
  }

  static String getSafetyThresholdHarassment(Context context) {
    return "BLOCK_LOW_AND_ABOVE";
  }

  static String getSafetyThresholdHateSpeech(Context context) {
    return "BLOCK_LOW_AND_ABOVE";
  }

  static String getSafetyThresholdSexuallyExplicit(Context context) {
    return "BLOCK_LOW_AND_ABOVE";
  }

  static String getSafetyThresholdDangerousContent(Context context) {
    return "BLOCK_LOW_AND_ABOVE";
  }

  public static boolean isServerSideGeminiImageCaptioningEnabled(Context context) {
    return isGeminiVoiceCommandEnabled(context);
  }

  public static boolean isOnDeviceGeminiImageCaptioningEnabled(Context context) {
    return false;
  }

  public static boolean useAratea(Context context) {
    return false;
  }
}
