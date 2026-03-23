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

package com.google.android.accessibility.talkback.actor.gemini;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;

/** Secure storage helpers for Gemini API key. */
public final class GeminiPrefs {
  private static final String TAG = "GeminiPrefs";
  private static final String SECURE_PREFS_NAME = "blindreader_gemini_secure";
  private static final Object LOCK = new Object();
  private static SharedPreferences securePrefs;

  private GeminiPrefs() {}

  public static String getApiKey(Context context) {
    SharedPreferences prefs = getSecurePrefs(context);
    return prefs.getString(context.getString(R.string.pref_gemini_api_key_key), "");
  }

  public static void setApiKey(Context context, String apiKey) {
    SharedPreferences prefs = getSecurePrefs(context);
    prefs
        .edit()
        .putString(context.getString(R.string.pref_gemini_api_key_key), apiKey)
        .apply();
  }

  public static boolean hasApiKey(Context context) {
    return !TextUtils.isEmpty(getApiKey(context));
  }

  private static SharedPreferences getSecurePrefs(Context context) {
    synchronized (LOCK) {
      if (securePrefs != null) {
        return securePrefs;
      }
      try {
        MasterKey masterKey =
            new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        securePrefs =
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
      } catch (Exception e) {
        LogUtils.e(TAG, "Falling back to standard SharedPreferences: %s", e.getMessage());
        securePrefs = SharedPreferencesUtils.getSharedPreferences(context);
      }
      return securePrefs;
    }
  }
}
