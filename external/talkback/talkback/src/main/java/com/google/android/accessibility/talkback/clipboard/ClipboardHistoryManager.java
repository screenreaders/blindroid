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

package com.google.android.accessibility.talkback.clipboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.checkerframework.checker.nullness.qual.Nullable;
import android.content.SharedPreferences;

/** Maintains a simple in-memory clipboard history for BlindReader. */
public final class ClipboardHistoryManager implements ClipboardManager.OnPrimaryClipChangedListener {

  public static final int DEFAULT_MAX_ITEMS = 10;

  private final Context context;
  private final ClipboardManager clipboardManager;
  private final ArrayDeque<ClipboardItem> history = new ArrayDeque<>();
  private final SharedPreferences prefs;

  private boolean enabled;
  private int maxItems = DEFAULT_MAX_ITEMS;
  private @Nullable String lastClipText;

  public ClipboardHistoryManager(Context context) {
    this.context = context.getApplicationContext();
    this.clipboardManager =
        (ClipboardManager) this.context.getSystemService(Context.CLIPBOARD_SERVICE);
    this.prefs = SharedPreferencesUtils.getSharedPreferences(this.context);
  }

  public void setEnabled(boolean enabled) {
    if (this.enabled == enabled) {
      return;
    }
    this.enabled = enabled;
    if (enabled) {
      loadHistory();
      clipboardManager.addPrimaryClipChangedListener(this);
      captureCurrentClipboard();
    } else {
      clipboardManager.removePrimaryClipChangedListener(this);
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setMaxItems(int maxItems) {
    if (maxItems <= 0) {
      return;
    }
    this.maxItems = maxItems;
    trimHistory();
    persistHistory();
  }

  public List<ClipboardItem> getHistory() {
    return new ArrayList<>(history);
  }

  public void clear() {
    history.clear();
    persistHistory();
  }

  public void shutdown() {
    persistHistory();
    setEnabled(false);
    history.clear();
  }

  public void setPrimaryClip(CharSequence text) {
    ClipData clip = ClipData.newPlainText("clipboard", text);
    clipboardManager.setPrimaryClip(clip);
  }

  public String buildMenuLabel(ClipboardItem item, int maxChars) {
    String normalized = normalizeText(item.getText());
    if (normalized.length() <= maxChars) {
      return normalized;
    }
    return normalized.substring(0, maxChars - 3) + "...";
  }

  @Override
  public void onPrimaryClipChanged() {
    if (!enabled) {
      return;
    }
    @Nullable CharSequence text = readPrimaryClipText();
    if (TextUtils.isEmpty(text)) {
      return;
    }
    String newText = text.toString();
    if (TextUtils.equals(lastClipText, newText)) {
      return;
    }
    lastClipText = newText;
    history.addFirst(new ClipboardItem(newText, System.currentTimeMillis()));
    trimHistory();
    persistHistory();
  }

  private void captureCurrentClipboard() {
    @Nullable CharSequence text = readPrimaryClipText();
    if (!TextUtils.isEmpty(text)) {
      String newText = text.toString();
      lastClipText = newText;
      history.addFirst(new ClipboardItem(newText, System.currentTimeMillis()));
      trimHistory();
      persistHistory();
    }
  }

  private @Nullable CharSequence readPrimaryClipText() {
    ClipData clip = clipboardManager.getPrimaryClip();
    if (clip == null || clip.getItemCount() == 0) {
      return null;
    }
    ClipData.Item item = clip.getItemAt(0);
    return item.coerceToText(context);
  }

  private void trimHistory() {
    while (history.size() > maxItems) {
      history.removeLast();
    }
  }

  private void loadHistory() {
    history.clear();
    String raw =
        prefs.getString(context.getString(R.string.pref_clipboard_history_store_key), null);
    if (TextUtils.isEmpty(raw)) {
      return;
    }
    try {
      JSONArray array = new JSONArray(raw);
      for (int i = 0; i < array.length(); i++) {
        JSONObject obj = array.optJSONObject(i);
        if (obj == null) {
          continue;
        }
        String text = obj.optString("text", "");
        long ts = obj.optLong("ts", 0L);
        if (TextUtils.isEmpty(text)) {
          continue;
        }
        history.addLast(new ClipboardItem(text, ts));
      }
      if (!history.isEmpty()) {
        lastClipText = history.peekFirst().getText();
      }
      trimHistory();
    } catch (JSONException e) {
      history.clear();
    }
  }

  private void persistHistory() {
    JSONArray array = new JSONArray();
    for (ClipboardItem item : history) {
      JSONObject obj = new JSONObject();
      try {
        obj.put("text", item.getText());
        obj.put("ts", item.getTimestamp());
        array.put(obj);
      } catch (JSONException e) {
        // Ignore broken entry.
      }
    }
    prefs
        .edit()
        .putString(context.getString(R.string.pref_clipboard_history_store_key), array.toString())
        .apply();
  }

  private static String normalizeText(String text) {
    String normalized = text.replace('\n', ' ').replace('\r', ' ').trim();
    while (normalized.contains("  ")) {
      normalized = normalized.replace("  ", " ");
    }
    return normalized;
  }

  /** A clipboard entry. */
  public static final class ClipboardItem {
    private final String text;
    private final long timestamp;

    public ClipboardItem(String text, long timestamp) {
      this.text = text;
      this.timestamp = timestamp;
    }

    public String getText() {
      return text;
    }

    public long getTimestamp() {
      return timestamp;
    }
  }
}
