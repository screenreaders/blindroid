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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.text.TextUtils;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/** This class implements the configuration flags for Gemini requests. */
public final class GeminiConfiguration {

  private GeminiConfiguration() {}

  private static final Object LOCK = new Object();
  private static volatile boolean initialized;
  private static @Nullable SharedPreferences cachedPrefs;
  private static @Nullable OnSharedPreferenceChangeListener prefListener;
  private static @Nullable Context appContext;

  private static boolean geminiEnabled;
  private static String geminiModel = "";
  private static String prefixPrompt = "";
  private static String readScreenPrompt = "";
  private static String readScreenStyle = "";
  private static boolean readScreenHaptic;

  public static void initialize(Context context) {
    synchronized (LOCK) {
      if (initialized) {
        return;
      }
      appContext = context.getApplicationContext();
      cachedPrefs = SharedPreferencesUtils.getSharedPreferences(appContext);
      refreshCache(appContext);
      prefListener =
          (prefs, key) -> {
            if (appContext == null) {
              return;
            }
            updateCache(appContext, key);
          };
      cachedPrefs.registerOnSharedPreferenceChangeListener(prefListener);
      initialized = true;
    }
  }

  public static void shutdown() {
    synchronized (LOCK) {
      if (!initialized) {
        return;
      }
      if (cachedPrefs != null && prefListener != null) {
        cachedPrefs.unregisterOnSharedPreferenceChangeListener(prefListener);
      }
      prefListener = null;
      cachedPrefs = null;
      appContext = null;
      initialized = false;
    }
  }

  public static boolean isGeminiVoiceCommandEnabled(Context context) {
    if (initialized) {
      return geminiEnabled;
    }
    return readGeminiEnabled(context);
  }

  private static boolean readGeminiEnabled(Context context) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    return SharedPreferencesUtils.getBooleanPref(
        prefs,
        context.getResources(),
        R.string.pref_gemini_enabled_key,
        R.bool.pref_gemini_opt_in_default);
  }

  static String getGeminiModel(Context context) {
    if (initialized) {
      return geminiModel;
    }
    return readGeminiModel(context);
  }

  static String getPrefixPrompt(Context context) {
    if (initialized) {
      return prefixPrompt;
    }
    return readPrefixPrompt(context);
  }

  static String getReadScreenPrompt(Context context) {
    String style = getReadScreenStyle(context);
    if (TextUtils.equals(
        style, context.getString(R.string.pref_gemini_read_screen_style_value_summary))) {
      return context.getString(R.string.gemini_read_screen_summary_prompt);
    }
    if (initialized) {
      return TextUtils.isEmpty(readScreenPrompt)
          ? context.getString(R.string.gemini_read_screen_default_prompt)
          : readScreenPrompt;
    }
    return readReadScreenPrompt(context);
  }

  static String getReadScreenStyle(Context context) {
    if (initialized) {
      return readScreenStyle;
    }
    return readReadScreenStyle(context);
  }

  static boolean isReadScreenHapticEnabled(Context context) {
    if (initialized) {
      return readScreenHaptic;
    }
    return readReadScreenHapticEnabled(context);
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

  private static void refreshCache(Context context) {
    geminiEnabled = readGeminiEnabled(context);
    geminiModel = readGeminiModel(context);
    prefixPrompt = readPrefixPrompt(context);
    readScreenPrompt = readReadScreenPrompt(context);
    readScreenStyle = readReadScreenStyle(context);
    readScreenHaptic = readReadScreenHapticEnabled(context);
  }

  private static void updateCache(Context context, @Nullable String key) {
    if (key == null) {
      refreshCache(context);
      return;
    }
    String enabledKey = context.getString(R.string.pref_gemini_enabled_key);
    String modelKey = context.getString(R.string.pref_gemini_model_key);
    String prefixKey = context.getString(R.string.pref_gemini_prefix_prompt_key);
    String promptKey = context.getString(R.string.pref_gemini_read_screen_prompt_key);
    String styleKey = context.getString(R.string.pref_gemini_read_screen_style_key);
    String hapticKey = context.getString(R.string.pref_gemini_read_screen_haptic_key);
    if (TextUtils.equals(key, enabledKey)) {
      geminiEnabled = readGeminiEnabled(context);
    } else if (TextUtils.equals(key, modelKey)) {
      geminiModel = readGeminiModel(context);
    } else if (TextUtils.equals(key, prefixKey)) {
      prefixPrompt = readPrefixPrompt(context);
    } else if (TextUtils.equals(key, promptKey)) {
      readScreenPrompt = readReadScreenPrompt(context);
    } else if (TextUtils.equals(key, styleKey)) {
      readScreenStyle = readReadScreenStyle(context);
    } else if (TextUtils.equals(key, hapticKey)) {
      readScreenHaptic = readReadScreenHapticEnabled(context);
    }
  }

  private static String readGeminiModel(Context context) {
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

  private static String readPrefixPrompt(Context context) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String value =
        SharedPreferencesUtils.getStringPref(
            prefs,
            context.getResources(),
            R.string.pref_gemini_prefix_prompt_key,
            R.string.pref_gemini_prefix_prompt_default);
    return value == null ? "" : value;
  }

  private static String readReadScreenPrompt(Context context) {
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

  private static String readReadScreenStyle(Context context) {
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

  private static boolean readReadScreenHapticEnabled(Context context) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    return SharedPreferencesUtils.getBooleanPref(
        prefs,
        context.getResources(),
        R.string.pref_gemini_read_screen_haptic_key,
        R.bool.pref_gemini_read_screen_haptic_default);
  }
}
