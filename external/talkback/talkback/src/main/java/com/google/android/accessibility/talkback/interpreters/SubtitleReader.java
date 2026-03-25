package com.google.android.accessibility.talkback.interpreters;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.AccessibilityEventListener;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.List;

/** Speaks detected subtitle/caption changes. */
public class SubtitleReader implements AccessibilityEventListener {
  private static final long MIN_REPEAT_MS = 1500L;
  private static final int EVENT_TYPES =
      AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;

  private final Context context;
  private final Pipeline.FeedbackReturner pipeline;
  private long lastSpokenAt = 0L;
  private String lastSpokenText = "";

  public SubtitleReader(Context context, Pipeline.FeedbackReturner pipeline) {
    this.context = context;
    this.pipeline = pipeline;
  }

  @Override
  public int getEventTypes() {
    return EVENT_TYPES;
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event, EventId eventId) {
    if (!isEnabled()) {
      return;
    }
    CharSequence text = extractText(event);
    if (TextUtils.isEmpty(text)) {
      return;
    }
    String normalized = normalize(text.toString());
    if (TextUtils.isEmpty(normalized)) {
      return;
    }
    AccessibilityNodeInfoCompat source = AccessibilityNodeInfoUtils.toCompat(event.getSource());
    if (!isLikelySubtitle(source)) {
      return;
    }
    if (isDuplicate(normalized)) {
      return;
    }
    lastSpokenText = normalized;
    lastSpokenAt = System.currentTimeMillis();
    pipeline.returnFeedback(eventId, Feedback.speech(normalized));
  }

  private boolean isEnabled() {
    return SharedPreferencesUtils.getBooleanPref(
        SharedPreferencesUtils.getSharedPreferences(context),
        context.getResources(),
        R.string.pref_subtitle_reader_key,
        R.bool.pref_subtitle_reader_default);
  }

  private CharSequence extractText(AccessibilityEvent event) {
    List<CharSequence> text = event.getText();
    if (text != null && !text.isEmpty()) {
      StringBuilder builder = new StringBuilder();
      for (CharSequence item : text) {
        if (!TextUtils.isEmpty(item)) {
          if (builder.length() > 0) {
            builder.append(' ');
          }
          builder.append(item);
        }
      }
      if (builder.length() > 0) {
        return builder.toString();
      }
    }
    return event.getContentDescription();
  }

  private boolean isLikelySubtitle(AccessibilityNodeInfoCompat node) {
    if (node == null) {
      return true;
    }
    if (node.isEditable()) {
      return false;
    }
    CharSequence className = node.getClassName();
    if (className != null && className.toString().contains("EditText")) {
      return false;
    }
    Rect bounds = new Rect();
    node.getBoundsInScreen(bounds);
    if (bounds.height() <= 0 || bounds.width() <= 0) {
      return false;
    }
    int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
    int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
    boolean bottomRegion = bounds.top >= (int) (screenHeight * 0.55f);
    boolean wideEnough = bounds.width() >= (int) (screenWidth * 0.35f);
    return bottomRegion && wideEnough;
  }

  private boolean isDuplicate(String text) {
    long now = System.currentTimeMillis();
    return text.equals(lastSpokenText) && (now - lastSpokenAt) < MIN_REPEAT_MS;
  }

  private String normalize(String text) {
    String trimmed = text.trim();
    if (trimmed.length() > 300) {
      trimmed = trimmed.substring(0, 300);
    }
    return trimmed;
  }
}
