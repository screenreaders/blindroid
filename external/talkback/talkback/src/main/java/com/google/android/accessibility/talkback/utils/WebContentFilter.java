package com.google.android.accessibility.talkback.utils;

import android.content.Context;
import android.text.TextUtils;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.regex.Pattern;

/** Utility for reducing noisy web content in spoken output. */
public final class WebContentFilter {
  private static final Pattern[] NOISE_PATTERNS =
      new Pattern[] {
        Pattern.compile("(?i)\\badvertisement\\b"),
        Pattern.compile("(?i)\\bsponsored\\b"),
        Pattern.compile("(?i)\\bskip to content\\b"),
        Pattern.compile("(?i)\\baccept all cookies\\b"),
        Pattern.compile("(?i)\\bcookie settings\\b"),
        Pattern.compile("(?i)\\bprivacy policy\\b"),
        Pattern.compile("(?i)\\bterms of service\\b")
      };

  private WebContentFilter() {}

  public static boolean isEnabled(Context context) {
    return SharedPreferencesUtils.getBooleanPref(
        SharedPreferencesUtils.getSharedPreferences(context),
        context.getResources(),
        R.string.pref_web_content_filter_key,
        R.bool.pref_web_content_filter_default);
  }

  public static CharSequence filter(Context context, CharSequence text) {
    if (TextUtils.isEmpty(text)) {
      return text;
    }
    String cleaned = text.toString();
    for (Pattern pattern : NOISE_PATTERNS) {
      cleaned = pattern.matcher(cleaned).replaceAll("");
    }
    cleaned = cleaned.replaceAll("\\s{2,}", " ").trim();
    return cleaned.isEmpty() ? text : cleaned;
  }
}
