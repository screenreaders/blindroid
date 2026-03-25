/*
 * Copyright (C) 2019 Google Inc.
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

package com.google.android.accessibility.talkback.gesture;

import static com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_FAKED_SPLIT_TYPING;
import static com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_TAP_HOLD_AND_2ND_FINGER_BACKWARD_DOUBLE_TAP;
import static com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_TAP_HOLD_AND_2ND_FINGER_FORWARD_DOUBLE_TAP;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.FingerprintGestureController;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.text.TextUtils;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.talkback.FeatureFlagReader;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.compositor.GestureShortcutProvider;
import com.google.android.accessibility.talkback.preference.PreferencesActivityUtils;
import com.google.android.accessibility.talkback.gesture.GestureSchemeStore;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.Logger;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.WindowUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The class provides gesture and action mappings in TalkBack for quick access. It updates cache
 * mappings whenever preference or screen layout changed.
 */
public class GestureShortcutMapping implements GestureShortcutProvider {
  private static final String TAG = GestureShortcutMapping.class.getSimpleName();

  /**
   * Type of gestures. The value is also used to prioritize gestures if multi-gestures are assigned
   * to one action. Smaller value has higher priority. For example, when creating the usage hint, we
   * prefer to use multi-finger gestures(value 0), then use single finger gestures(value 1) if no
   * multi-finger gestures are found.
   */
  @IntDef({MULTI_FINGER, SINGLE_FINGER, FINGERPRINT})
  @Retention(RetentionPolicy.SOURCE)
  private @interface GestureType {}

  private static final int MULTI_FINGER = 0;
  private static final int SINGLE_FINGER = 1;
  private static final int FINGERPRINT = 2;

  /** Defines how the gesture works with right-to-left screen. */
  @IntDef({RTL_UNRELATED, LTR_GESTURE, RTL_GESTURE})
  @Retention(RetentionPolicy.SOURCE)
  private @interface RTLType {}

  private static final int RTL_UNRELATED = 0;
  private static final int LTR_GESTURE = 1;
  private static final int RTL_GESTURE = 2;

  // The number of gesture sets to be provided.
  private static final int NUMBER_OF_GESTURE_SET = 2;

  // Cache action strings per Resources to avoid repeated linear scans.
  private static final Object ACTION_CACHE_LOCK = new Object();
  private static final WeakHashMap<android.content.res.Resources, Map<String, String>>
      ACTION_STRING_CACHE = new WeakHashMap<>();
  private static volatile int ACTION_CACHE_CONFIG_HASH = 0;

  /** List all of supported gestures. */
  private enum TalkBackGesture {
    SWIPE_UP(
        AccessibilityService.GESTURE_SWIPE_UP,
        SINGLE_FINGER,
        R.string.pref_shortcut_up_key,
        R.string.pref_shortcut_up_default),
    SWIPE_DOWN(
        AccessibilityService.GESTURE_SWIPE_DOWN,
        SINGLE_FINGER,
        R.string.pref_shortcut_down_key,
        R.string.pref_shortcut_down_default),
    SWIPE_LEFT(
        AccessibilityService.GESTURE_SWIPE_LEFT,
        SINGLE_FINGER,
        LTR_GESTURE,
        R.string.pref_shortcut_left_key,
        R.string.pref_shortcut_left_default),
    SWIPE_LEFT_RTL(
        AccessibilityService.GESTURE_SWIPE_LEFT,
        SINGLE_FINGER,
        RTL_GESTURE,
        R.string.pref_shortcut_right_key,
        R.string.pref_shortcut_right_default),
    SWIPE_RIGHT(
        AccessibilityService.GESTURE_SWIPE_RIGHT,
        SINGLE_FINGER,
        LTR_GESTURE,
        R.string.pref_shortcut_right_key,
        R.string.pref_shortcut_right_default),
    SWIPE_RIGHT_RTL(
        AccessibilityService.GESTURE_SWIPE_RIGHT,
        SINGLE_FINGER,
        RTL_GESTURE,
        R.string.pref_shortcut_left_key,
        R.string.pref_shortcut_left_default),
    SWIPE_UP_AND_DOWN(
        AccessibilityService.GESTURE_SWIPE_UP_AND_DOWN,
        SINGLE_FINGER,
        R.string.pref_shortcut_up_and_down_key,
        R.string.pref_shortcut_up_and_down_default),
    SWIPE_DOWN_AND_UP(
        AccessibilityService.GESTURE_SWIPE_DOWN_AND_UP,
        SINGLE_FINGER,
        R.string.pref_shortcut_down_and_up_key,
        R.string.pref_shortcut_down_and_up_default),
    SWIPE_LEFT_AND_RIGHT(
        AccessibilityService.GESTURE_SWIPE_LEFT_AND_RIGHT,
        SINGLE_FINGER,
        LTR_GESTURE,
        R.string.pref_shortcut_left_and_right_key,
        R.string.pref_shortcut_left_and_right_default),
    SWIPE_LEFT_AND_RIGHT_RTL(
        AccessibilityService.GESTURE_SWIPE_LEFT_AND_RIGHT,
        SINGLE_FINGER,
        RTL_GESTURE,
        R.string.pref_shortcut_right_and_left_key,
        R.string.pref_shortcut_right_and_left_default),
    SWIPE_RIGHT_AND_LEFT(
        AccessibilityService.GESTURE_SWIPE_RIGHT_AND_LEFT,
        SINGLE_FINGER,
        LTR_GESTURE,
        R.string.pref_shortcut_right_and_left_key,
        R.string.pref_shortcut_right_and_left_default),
    SWIPE_RIGHT_AND_LEFT_RTL(
        AccessibilityService.GESTURE_SWIPE_RIGHT_AND_LEFT,
        SINGLE_FINGER,
        RTL_GESTURE,
        R.string.pref_shortcut_left_and_right_key,
        R.string.pref_shortcut_left_and_right_default),
    SWIPE_UP_AND_LEFT(
        AccessibilityService.GESTURE_SWIPE_UP_AND_LEFT,
        SINGLE_FINGER,
        R.string.pref_shortcut_up_and_left_key,
        R.string.pref_shortcut_up_and_left_default),
    SWIPE_UP_AND_RIGHT(
        AccessibilityService.GESTURE_SWIPE_UP_AND_RIGHT,
        SINGLE_FINGER,
        R.string.pref_shortcut_up_and_right_key,
        R.string.pref_shortcut_up_and_right_default),
    SWIPE_DOWN_AND_LEFT(
        AccessibilityService.GESTURE_SWIPE_DOWN_AND_LEFT,
        SINGLE_FINGER,
        R.string.pref_shortcut_down_and_left_key,
        R.string.pref_shortcut_down_and_left_default),
    SWIPE_DOWN_AND_RIGHT(
        AccessibilityService.GESTURE_SWIPE_DOWN_AND_RIGHT,
        SINGLE_FINGER,
        R.string.pref_shortcut_down_and_right_key,
        R.string.pref_shortcut_down_and_right_default),
    SWIPE_RIGHT_AND_DOWN(
        AccessibilityService.GESTURE_SWIPE_RIGHT_AND_DOWN,
        SINGLE_FINGER,
        R.string.pref_shortcut_right_and_down_key,
        R.string.pref_shortcut_right_and_down_default),
    SWIPE_RIGHT_AND_UP(
        AccessibilityService.GESTURE_SWIPE_RIGHT_AND_UP,
        SINGLE_FINGER,
        R.string.pref_shortcut_right_and_up_key,
        R.string.pref_shortcut_right_and_up_default),
    SWIPE_LEFT_AND_DOWN(
        AccessibilityService.GESTURE_SWIPE_LEFT_AND_DOWN,
        SINGLE_FINGER,
        R.string.pref_shortcut_left_and_down_key,
        R.string.pref_shortcut_left_and_down_default),
    SWIPE_LEFT_AND_UP(
        AccessibilityService.GESTURE_SWIPE_LEFT_AND_UP,
        SINGLE_FINGER,
        R.string.pref_shortcut_left_and_up_key,
        R.string.pref_shortcut_left_and_up_default),
    // One-finger Tap
    ONE_FINGER_DOUBLE_TAP(
        AccessibilityService.GESTURE_DOUBLE_TAP,
        MULTI_FINGER,
        R.string.pref_shortcut_1finger_2tap_key,
        R.string.pref_shortcut_1finger_2tap_default),
    ONE_FINGER_DOUBLE_TAP_AND_HOLD(
        AccessibilityService.GESTURE_DOUBLE_TAP_AND_HOLD,
        MULTI_FINGER,
        R.string.pref_shortcut_1finger_2tap_hold_key,
        R.string.pref_shortcut_1finger_2tap_hold_default),
    EDGE_SWIPE_RIGHT(
        GESTURE_EDGE_SWIPE_RIGHT,
        SINGLE_FINGER,
        R.string.pref_shortcut_edge_swipe_right_key,
        R.string.pref_shortcut_edge_swipe_right_default),
    EDGE_SWIPE_LEFT(
        GESTURE_EDGE_SWIPE_LEFT,
        SINGLE_FINGER,
        R.string.pref_shortcut_edge_swipe_left_key,
        R.string.pref_shortcut_edge_swipe_left_default),
    // Multi-finger Gestures
    TWO_FINGER_SINGLE_TAP(
        AccessibilityService.GESTURE_2_FINGER_SINGLE_TAP,
        MULTI_FINGER,
        R.string.pref_shortcut_2finger_1tap_key,
        R.string.pref_shortcut_2finger_1tap_default),
    TWO_FINGER_DOUBLE_TAP(
        AccessibilityService.GESTURE_2_FINGER_DOUBLE_TAP,
        MULTI_FINGER,
        R.string.pref_shortcut_2finger_2tap_key,
        R.string.pref_shortcut_2finger_2tap_default),
    TWO_FINGER_TRIPLE_TAP(
        AccessibilityService.GESTURE_2_FINGER_TRIPLE_TAP,
        MULTI_FINGER,
        R.string.pref_shortcut_2finger_3tap_key,
        R.string.pref_shortcut_2finger_3tap_default),
    THREE_FINGER_SINGLE_TAP(
        AccessibilityService.GESTURE_3_FINGER_SINGLE_TAP,
        MULTI_FINGER,
        R.string.pref_shortcut_3finger_1tap_key,
        R.string.pref_shortcut_3finger_1tap_default),
    THREE_FINGER_DOUBLE_TAP(
        AccessibilityService.GESTURE_3_FINGER_DOUBLE_TAP,
        MULTI_FINGER,
        R.string.pref_shortcut_3finger_2tap_key,
        R.string.pref_shortcut_3finger_2tap_default),
    THREE_FINGER_TRIPLE_TAP(
        AccessibilityService.GESTURE_3_FINGER_TRIPLE_TAP,
        MULTI_FINGER,
        R.string.pref_shortcut_3finger_3tap_key,
        R.string.pref_shortcut_3finger_3tap_default),
    THREE_FINGER_TRIPLE_TAP_AND_HOLD(
        AccessibilityService.GESTURE_3_FINGER_TRIPLE_TAP_AND_HOLD,
        MULTI_FINGER,
        R.string.pref_shortcut_3finger_3tap_hold_key,
        R.string.pref_shortcut_3finger_3tap_hold_default),
    FOUR_FINGER_SINGLE_TAP(
        AccessibilityService.GESTURE_4_FINGER_SINGLE_TAP,
        MULTI_FINGER,
        R.string.pref_shortcut_4finger_1tap_key,
        R.string.pref_shortcut_4finger_1tap_default),
    FOUR_FINGER_DOUBLE_TAP(
        AccessibilityService.GESTURE_4_FINGER_DOUBLE_TAP,
        MULTI_FINGER,
        R.string.pref_shortcut_4finger_2tap_key,
        R.string.pref_shortcut_4finger_2tap_default),
    FOUR_FINGER_TRIPLE_TAP(
        AccessibilityService.GESTURE_4_FINGER_TRIPLE_TAP,
        MULTI_FINGER,
        R.string.pref_shortcut_4finger_3tap_key,
        R.string.pref_shortcut_4finger_3tap_default),
    TWO_FINGER_SWIPE_UP(
        AccessibilityService.GESTURE_2_FINGER_SWIPE_UP,
        MULTI_FINGER,
        R.string.pref_shortcut_2finger_swipe_up_key,
        R.string.pref_shortcut_2finger_swipe_up_default),
    TWO_FINGER_SWIPE_DOWN(
        AccessibilityService.GESTURE_2_FINGER_SWIPE_DOWN,
        MULTI_FINGER,
        R.string.pref_shortcut_2finger_swipe_down_key,
        R.string.pref_shortcut_2finger_swipe_down_default),
    TWO_FINGER_SWIPE_LEFT(
        AccessibilityService.GESTURE_2_FINGER_SWIPE_LEFT,
        MULTI_FINGER,
        R.string.pref_shortcut_2finger_swipe_left_key,
        R.string.pref_shortcut_2finger_swipe_left_default),
    TWO_FINGER_SWIPE_RIGHT(
        AccessibilityService.GESTURE_2_FINGER_SWIPE_RIGHT,
        MULTI_FINGER,
        R.string.pref_shortcut_2finger_swipe_right_key,
        R.string.pref_shortcut_2finger_swipe_right_default),
    TWO_FINGER_ROTATE_CW(
        GESTURE_ROTATE_CW,
        MULTI_FINGER,
        R.string.pref_shortcut_2finger_rotate_cw_key,
        R.string.pref_shortcut_2finger_rotate_cw_default),
    TWO_FINGER_ROTATE_CCW(
        GESTURE_ROTATE_CCW,
        MULTI_FINGER,
        R.string.pref_shortcut_2finger_rotate_ccw_key,
        R.string.pref_shortcut_2finger_rotate_ccw_default),
    THREE_FINGER_SWIPE_UP(
        AccessibilityService.GESTURE_3_FINGER_SWIPE_UP,
        MULTI_FINGER,
        R.string.pref_shortcut_3finger_swipe_up_key,
        R.string.pref_shortcut_3finger_swipe_up_default),
    THREE_FINGER_SWIPE_DOWN(
        AccessibilityService.GESTURE_3_FINGER_SWIPE_DOWN,
        MULTI_FINGER,
        R.string.pref_shortcut_3finger_swipe_down_key,
        R.string.pref_shortcut_3finger_swipe_down_default),
    THREE_FINGER_SWIPE_LEFT(
        AccessibilityService.GESTURE_3_FINGER_SWIPE_LEFT,
        MULTI_FINGER,
        R.string.pref_shortcut_3finger_swipe_left_key,
        R.string.pref_shortcut_3finger_swipe_left_default),
    THREE_FINGER_SWIPE_RIGHT(
        AccessibilityService.GESTURE_3_FINGER_SWIPE_RIGHT,
        MULTI_FINGER,
        R.string.pref_shortcut_3finger_swipe_right_key,
        R.string.pref_shortcut_3finger_swipe_right_default),
    FOUR_FINGER_SWIPE_UP(
        AccessibilityService.GESTURE_4_FINGER_SWIPE_UP,
        MULTI_FINGER,
        R.string.pref_shortcut_4finger_swipe_up_key,
        R.string.pref_shortcut_4finger_swipe_up_default),
    FOUR_FINGER_SWIPE_DOWN(
        AccessibilityService.GESTURE_4_FINGER_SWIPE_DOWN,
        MULTI_FINGER,
        R.string.pref_shortcut_4finger_swipe_down_key,
        R.string.pref_shortcut_4finger_swipe_down_default),
    FOUR_FINGER_SWIPE_LEFT(
        AccessibilityService.GESTURE_4_FINGER_SWIPE_LEFT,
        MULTI_FINGER,
        R.string.pref_shortcut_4finger_swipe_left_key,
        R.string.pref_shortcut_4finger_swipe_left_default),
    FOUR_FINGER_SWIPE_RIGHT(
        AccessibilityService.GESTURE_4_FINGER_SWIPE_RIGHT,
        MULTI_FINGER,
        R.string.pref_shortcut_4finger_swipe_right_key,
        R.string.pref_shortcut_4finger_swipe_right_default),
    TWO_FINGER_DOUBLE_TAP_AND_HOLD(
        AccessibilityService.GESTURE_2_FINGER_DOUBLE_TAP_AND_HOLD,
        MULTI_FINGER,
        R.string.pref_shortcut_2finger_2tap_hold_key,
        R.string.pref_shortcut_2finger_2tap_hold_default),
    THREE_FINGER_TAP_AND_HOLD(
        AccessibilityService.GESTURE_3_FINGER_SINGLE_TAP_AND_HOLD,
        MULTI_FINGER,
        R.string.pref_shortcut_3finger_1tap_hold_key,
        R.string.pref_shortcut_3finger_1tap_hold_default),
    THREE_FINGER_DOUBLE_TAP_AND_HOLD(
        AccessibilityService.GESTURE_3_FINGER_DOUBLE_TAP_AND_HOLD,
        MULTI_FINGER,
        R.string.pref_shortcut_3finger_2tap_hold_key,
        R.string.pref_shortcut_3finger_2tap_hold_default),
    FOUR_FINGER_DOUBLE_TAP_AND_HOLD(
        AccessibilityService.GESTURE_4_FINGER_DOUBLE_TAP_AND_HOLD,
        MULTI_FINGER,
        R.string.pref_shortcut_4finger_2tap_hold_key,
        R.string.pref_shortcut_4finger_2tap_hold_default),
    TWO_FINGER_TRIPLE_TAP_AND_HOLD(
        AccessibilityService.GESTURE_2_FINGER_TRIPLE_TAP_AND_HOLD,
        MULTI_FINGER,
        R.string.pref_shortcut_2finger_3tap_hold_key,
        R.string.pref_shortcut_2finger_3tap_hold_default),

    // Fingerprint.
    FINGERPRINT_SWIPE_UP(
        FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_UP,
        FINGERPRINT,
        R.string.pref_shortcut_fingerprint_up_key,
        R.string.pref_shortcut_fingerprint_up_default),
    FINGERPRINT_SWIPE_DOWN(
        FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_DOWN,
        FINGERPRINT,
        R.string.pref_shortcut_fingerprint_down_key,
        R.string.pref_shortcut_fingerprint_down_default),
    FINGERPRINT_SWIPE_LEFT(
        FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_LEFT,
        FINGERPRINT,
        R.string.pref_shortcut_fingerprint_left_key,
        R.string.pref_shortcut_fingerprint_left_default),
    FINGERPRINT_SWIPE_RIGHT(
        FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_RIGHT,
        FINGERPRINT,
        R.string.pref_shortcut_fingerprint_right_key,
        R.string.pref_shortcut_fingerprint_right_default);

    TalkBackGesture(int gestureId, @GestureType int gestureType, int keyId, int defaultActionId) {
      this(gestureId, gestureType, RTL_UNRELATED, keyId, defaultActionId);
    }

    TalkBackGesture(
        int gestureId,
        @GestureType int gestureType,
        @RTLType int rtlType,
        int keyId,
        int defaultActionId) {
      this.gestureId = gestureId;
      this.gestureType = gestureType;
      this.rtlType = rtlType;
      this.keyId = keyId;
      this.defaultActionId = defaultActionId;
    }

    final int gestureId;
    @GestureType final int gestureType;
    @RTLType final int rtlType;
    /**
     * For mapping the gesture id to action, we need to consider the variance when gesture set is
     * introduced. When gesture set 0 (default set) is activated, the mapping key is the same as
     * this keyId; otherwise, it needs to append with a '-' and gesture set to resolve the authentic
     * mapped action.
     */
    final int keyId;

    final int defaultActionId;
  }

  private static final int GESTURE_SET_IOS = 1;
  private static final EnumMap<TalkBackGesture, Integer> IOS_DEFAULT_ACTIONS =
      new EnumMap<>(TalkBackGesture.class);

  static {
    IOS_DEFAULT_ACTIONS.put(TalkBackGesture.SWIPE_RIGHT, R.string.shortcut_value_next);
    IOS_DEFAULT_ACTIONS.put(TalkBackGesture.SWIPE_RIGHT_RTL, R.string.shortcut_value_next);
    IOS_DEFAULT_ACTIONS.put(TalkBackGesture.SWIPE_LEFT, R.string.shortcut_value_previous);
    IOS_DEFAULT_ACTIONS.put(TalkBackGesture.SWIPE_LEFT_RTL, R.string.shortcut_value_previous);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.SWIPE_UP, R.string.shortcut_value_selected_setting_previous_action);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.SWIPE_DOWN, R.string.shortcut_value_selected_setting_next_action);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.TWO_FINGER_SINGLE_TAP, R.string.shortcut_value_pause_or_resume_feedback);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.TWO_FINGER_DOUBLE_TAP, R.string.shortcut_value_media_control);
    IOS_DEFAULT_ACTIONS.put(TalkBackGesture.SWIPE_LEFT_AND_RIGHT, R.string.shortcut_value_back);
    IOS_DEFAULT_ACTIONS.put(TalkBackGesture.SWIPE_RIGHT_AND_LEFT, R.string.shortcut_value_back);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.ONE_FINGER_DOUBLE_TAP_AND_HOLD,
        R.string.shortcut_value_pass_through_next_gesture);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.TWO_FINGER_SWIPE_UP, R.string.shortcut_value_read_from_top);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.TWO_FINGER_SWIPE_DOWN, R.string.shortcut_value_read_from_current);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.TWO_FINGER_SWIPE_LEFT, R.string.shortcut_value_prev_container);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.TWO_FINGER_SWIPE_RIGHT, R.string.shortcut_value_next_container);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.TWO_FINGER_ROTATE_CW, R.string.shortcut_value_select_next_setting);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.TWO_FINGER_ROTATE_CCW, R.string.shortcut_value_select_previous_setting);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.TWO_FINGER_TRIPLE_TAP, R.string.shortcut_value_item_chooser);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.TWO_FINGER_TRIPLE_TAP_AND_HOLD, R.string.shortcut_value_quick_settings);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.TWO_FINGER_DOUBLE_TAP_AND_HOLD,
        R.string.shortcut_value_show_custom_actions);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.THREE_FINGER_TAP_AND_HOLD,
        R.string.shortcut_value_split_tap_activate);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.THREE_FINGER_SINGLE_TAP, R.string.shortcut_value_announce_item_position);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.THREE_FINGER_DOUBLE_TAP, R.string.shortcut_value_toggle_voice_feedback);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.THREE_FINGER_TRIPLE_TAP, R.string.shortcut_value_show_hide_screen);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.THREE_FINGER_SWIPE_UP, R.string.shortcut_value_scroll_down);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.THREE_FINGER_SWIPE_DOWN, R.string.shortcut_value_scroll_up);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.THREE_FINGER_SWIPE_LEFT, R.string.shortcut_value_scroll_forward);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.THREE_FINGER_SWIPE_RIGHT, R.string.shortcut_value_scroll_back);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.FOUR_FINGER_SINGLE_TAP, R.string.shortcut_value_first_in_screen);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.FOUR_FINGER_DOUBLE_TAP, R.string.shortcut_value_last_in_screen);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.FOUR_FINGER_SWIPE_UP, R.string.shortcut_value_first_in_screen);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.FOUR_FINGER_SWIPE_DOWN, R.string.shortcut_value_last_in_screen);
    IOS_DEFAULT_ACTIONS.put(
        TalkBackGesture.FOUR_FINGER_TRIPLE_TAP, R.string.shortcut_value_live_recognition);
  }

  /** All supported actions. */
  public enum TalkbackAction {
    UNASSIGNED_ACTION(-1, -1),
    // Basic navigation.
    PERFORM_CLICK(
        R.string.shortcut_value_perform_click_action, R.string.shortcut_perform_click_action),
    SPLIT_TAP_ACTIVATE(
        R.string.shortcut_value_split_tap_activate, R.string.shortcut_split_tap_activate),
    PERFORM_LONG_CLICK(
        R.string.shortcut_value_perform_click_action, R.string.shortcut_perform_long_click_action),
    PREVIOUS(R.string.shortcut_value_previous, R.string.shortcut_previous),
    NEXT(R.string.shortcut_value_next, R.string.shortcut_next),
    FIRST_IN_SCREEN(R.string.shortcut_value_first_in_screen, R.string.shortcut_first_in_screen),
    LAST_IN_SCREEN(R.string.shortcut_value_last_in_screen, R.string.shortcut_last_in_screen),
    PREV_CONTAINER(R.string.shortcut_value_prev_container, R.string.shortcut_prev_container),
    NEXT_CONTAINER(R.string.shortcut_value_next_container, R.string.shortcut_next_container),
    PREVIOUS_WINDOW(R.string.shortcut_value_previous_window, R.string.shortcut_previous_window),
    NEXT_WINDOW(R.string.shortcut_value_next_window, R.string.shortcut_next_window),
    SCROLL_BACK(R.string.shortcut_value_scroll_back, R.string.shortcut_scroll_back),
    SCROLL_FORWARD(R.string.shortcut_value_scroll_forward, R.string.shortcut_scroll_forward),
    SCROLL_UP(R.string.shortcut_value_scroll_up, R.string.shortcut_scroll_up),
    SCROLL_DOWN(R.string.shortcut_value_scroll_down, R.string.shortcut_scroll_down),
    SCROLL_LEFT(R.string.shortcut_value_scroll_left, R.string.shortcut_scroll_left),
    SCROLL_RIGHT(R.string.shortcut_value_scroll_right, R.string.shortcut_scroll_right),

    // System action.
    HOME(R.string.shortcut_value_home, R.string.shortcut_home),
    BACK(R.string.shortcut_value_back, R.string.shortcut_back),
    OVERVIEW(R.string.shortcut_value_overview, R.string.shortcut_overview),
    NOTIFICATIONS(R.string.shortcut_value_notifications, R.string.shortcut_notifications),
    QUICK_SETTINGS(R.string.shortcut_value_quick_settings, R.string.shortcut_quick_settings),
    ALL_APPS(R.string.shortcut_value_all_apps, R.string.shortcut_all_apps),
    A11Y_BUTTON(R.string.shortcut_value_a11y_button, R.string.shortcut_a11y_button),
    A11Y_BUTTON_LONG_PRESS(
        R.string.shortcut_value_a11y_button_long_press, R.string.shortcut_a11y_button_long_press),

    // Reading control.
    READ_FROM_TOP(R.string.shortcut_value_read_from_top, R.string.shortcut_read_from_top),
    READ_FROM_CURRENT(
        R.string.shortcut_value_read_from_current, R.string.shortcut_read_from_current),
    PAUSE_OR_RESUME_FEEDBACK(
        R.string.shortcut_value_pause_or_resume_feedback,
        R.string.shortcut_pause_or_resume_feedback),
    TOGGLE_VOICE_FEEDBACK(
        R.string.shortcut_value_toggle_voice_feedback, R.string.shortcut_toggle_voice_feedback),
    TOGGLE_EXPLORE_BY_TOUCH(
        R.string.shortcut_value_toggle_explore_by_touch,
        R.string.shortcut_toggle_explore_by_touch),
    TOGGLE_SINGLE_TAP(
        R.string.shortcut_value_toggle_single_tap, R.string.shortcut_toggle_single_tap),
    TOGGLE_WEB_SCRIPTS(
        R.string.shortcut_value_toggle_web_scripts, R.string.shortcut_toggle_web_scripts),
    TOGGLE_SPEAK_NOTIFICATIONS(
        R.string.shortcut_value_toggle_speak_notifications,
        R.string.shortcut_toggle_speak_notifications),
    MACRO_1(R.string.shortcut_value_macro_1, R.string.shortcut_macro_1),
    MACRO_2(R.string.shortcut_value_macro_2, R.string.shortcut_macro_2),
    MACRO_3(R.string.shortcut_value_macro_3, R.string.shortcut_macro_3),
    DOCUMENT_SCAN(
        R.string.shortcut_value_document_scan, R.string.shortcut_document_scan),
    SCAN_HUB(
        R.string.shortcut_value_scan_hub, R.string.shortcut_scan_hub),
    SHOW_LANGUAGE_OPTIONS(
        R.string.shortcut_value_show_language_options, R.string.shortcut_show_language_options),

    // Menu control.
    TALKBACK_BREAKOUT(
        R.string.shortcut_value_talkback_breakout, R.string.shortcut_talkback_breakout),
    SELECT_PREVIOUS_SETTING(
        R.string.shortcut_value_select_previous_setting, R.string.shortcut_select_previous_setting),
    SELECT_NEXT_SETTING(
        R.string.shortcut_value_select_next_setting, R.string.shortcut_select_next_setting),
    SELECTED_SETTING_PREVIOUS_ACTION(
        R.string.shortcut_value_selected_setting_previous_action,
        R.string.shortcut_selected_setting_previous_action),
    SELECTED_SETTING_NEXT_ACTION(
        R.string.shortcut_value_selected_setting_next_action,
        R.string.shortcut_selected_setting_next_action),
    QUICK_MENU(R.string.shortcut_value_quick_menu, R.string.shortcut_quick_menu),
    ITEM_CHOOSER(R.string.shortcut_value_item_chooser, R.string.shortcut_item_chooser),
    CLIPBOARD_HISTORY(
        R.string.shortcut_value_clipboard_history, R.string.shortcut_clipboard_history),
    GESTURE_SCHEME_MANAGER(
        R.string.shortcut_value_gesture_scheme_manager, R.string.shortcut_gesture_scheme_manager),
    BACKUP_SETTINGS(
        R.string.shortcut_value_backup_settings, R.string.shortcut_backup_settings),
    TRANSLATE_TEXT(
        R.string.shortcut_value_translate_text, R.string.shortcut_translate_text),
    OBJECT_RECOGNITION(
        R.string.shortcut_value_object_recognition, R.string.shortcut_object_recognition),
    FACE_RECOGNITION(
        R.string.shortcut_value_face_recognition, R.string.shortcut_face_recognition),
    MONEY_RECOGNITION(
        R.string.shortcut_value_money_recognition, R.string.shortcut_money_recognition),
    SCENE_RECOGNITION(
        R.string.shortcut_value_scene_recognition, R.string.shortcut_scene_recognition),
    LIVE_RECOGNITION(
        R.string.shortcut_value_live_recognition, R.string.shortcut_live_recognition),
    TOGGLE_PER_APP_GESTURE_SET(
        R.string.shortcut_value_toggle_per_app_gesture_set,
        R.string.shortcut_toggle_per_app_gesture_set),
    ANNOUNCE_ITEM_POSITION(
        R.string.shortcut_value_announce_item_position,
        R.string.shortcut_announce_item_position),
    REPEAT_LAST_UTTERANCE(
        R.string.shortcut_value_repeat_last_utterance,
        R.string.shortcut_repeat_last_utterance),
    SPELL_LAST_UTTERANCE(
        R.string.shortcut_value_spell_last_utterance,
        R.string.shortcut_spell_last_utterance),
    ANNOUNCE_TIME(R.string.shortcut_value_announce_time, R.string.shortcut_announce_time),
    ANNOUNCE_BATTERY(
        R.string.shortcut_value_announce_battery, R.string.shortcut_announce_battery),

    // Text editing.
    START_SELECTION_MODE(
        R.string.shortcut_value_start_selection_mode,
        R.string.title_edittext_breakout_start_selection_mode),
    MOVE_CURSOR_TO_BEGINNING(
        R.string.shortcut_value_move_cursor_to_beginning,
        R.string.title_edittext_breakout_move_to_beginning),
    MOVE_CURSOR_TO_END(
        R.string.shortcut_value_move_cursor_to_end, R.string.title_edittext_breakout_move_to_end),
    SELECT_ALL(R.string.shortcut_value_select_all, android.R.string.selectAll),
    COPY(R.string.shortcut_value_copy, android.R.string.copy),
    CUT(R.string.shortcut_value_cut, android.R.string.cut),
    PASTE(R.string.shortcut_value_paste, android.R.string.paste),
    COPY_LAST_SPOKEN_UTTERANCE(
        R.string.shortcut_value_copy_last_spoken_phrase, R.string.title_copy_last_spoken_phrase),
    BRAILLE_KEYBOARD(R.string.shortcut_value_braille_keyboard, R.string.shortcut_braille_keyboard),

    // Special features.
    MEDIA_CONTROL(R.string.shortcut_value_media_control, R.string.shortcut_media_control),
    INCREASE_VOLUME(R.string.shortcut_value_increase_volume, R.string.shortcut_increase_volume),
    DECREASE_VOLUME(R.string.shortcut_value_decrease_volume, R.string.shortcut_decrease_volume),
    VOICE_COMMANDS(R.string.shortcut_value_voice_commands, R.string.shortcut_voice_commands),
    SCREEN_SEARCH(R.string.shortcut_value_screen_search, R.string.title_show_screen_search),
    SHOW_HIDE_SCREEN(R.string.shortcut_value_show_hide_screen, R.string.title_show_hide_screen),
    PASS_THROUGH_NEXT_GESTURE(
        R.string.shortcut_value_pass_through_next_gesture, R.string.shortcut_pass_through_next),
    PRINT_NODE_TREE(R.string.shortcut_value_print_node_tree, R.string.shortcut_print_node_tree),
    PRINT_PERFORMANCE_STATS(
        R.string.shortcut_value_print_performance_stats, R.string.shortcut_print_performance_stats),
    SHOW_CUSTOM_ACTIONS(
        R.string.shortcut_value_show_custom_actions, R.string.shortcut_show_custom_actions),
    NAVIGATE_BRAILLE_SETTINGS(
        R.string.shortcut_value_braille_display_settings,
        R.string.shortcut_braille_display_settings),
    TUTORIAL(R.string.shortcut_value_tutorial, R.string.shortcut_tutorial),
    PRACTICE_GESTURE(
        R.string.shortcut_value_practice_gestures, R.string.shortcut_practice_gestures),
    REPORT_GESTURE(R.string.shortcut_value_report_gesture, R.string.shortcut_report_gesture),
    TOGGLE_BRAILLE_DISPLAY_ON_OFF(
        R.string.shortcut_value_toggle_braille_display, R.string.shortcut_toggle_braille_display),
    DESCRIBE_IMAGE(R.string.shortcut_value_describe_image, R.string.title_image_caption);

    @StringRes final int actionKeyResId;
    @StringRes final int actionNameResId;

    TalkbackAction(@StringRes int actionKeyResId, @StringRes int actionNameResId) {
      this.actionKeyResId = actionKeyResId;
      this.actionNameResId = actionNameResId;
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // Constants

  // Copies hidden constants from framework.
  /**
   * The user has performed a touch-exploration gesture on the touch screen without ever triggering
   * gesture detection. This gesture is only dispatched when {@link
   * FeatureSupport#FLAG_SEND_MOTION_EVENTS} is set.
   */
  public static final int GESTURE_TOUCH_EXPLORATION = -2;

  /**
   * The user has performed a passthrough gesture on the touch screen without ever triggering
   * gesture detection. This gesture is only dispatched when {@link
   * FeatureSupport#FLAG_SEND_MOTION_EVENTS} is set.
   */
  public static final int GESTURE_PASSTHROUGH = -1;
  public static final int GESTURE_EDGE_SWIPE_RIGHT = -10;
  public static final int GESTURE_EDGE_SWIPE_LEFT = -11;
  public static final int GESTURE_ROTATE_CW = -12;
  public static final int GESTURE_ROTATE_CCW = -13;

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Member variables

  protected String actionUnassigned;
  protected String actionTalkbackContextMenu;
  protected String actionNextReadingMenuSetting;
  protected String actionReadingMenuUp;
  protected String actionReadingMenuDown;
  protected String actionShortcut;
  protected String nextWindowShortcut;
  protected String mediaControlShortcut;

  private final String actionGestureUnsupported;

  private Context context;
  private boolean gestureSetEnabled;
  private boolean perAppGestureSetEnabled;
  private boolean invertSwipeGestures;
  private boolean multiPartGesturesEnabled;
  // Specify which gesture set (0/1) is activated. Default value is 0.
  private int currentGestureSet;
  @Nullable private String currentPackageName;
  @Nullable private Integer currentPackageGestureSetOverride;
  private final SharedPreferences prefs;
  private int previousScreenLayout = 0;
  private final List<HashMap<String, GestureCollector>> actionToGesture = new ArrayList<>();
  private final List<HashMap<Integer, String>> gestureIdToActionKey = new ArrayList<>();
  private HashMap<Integer, String> fingerprintGestureIdToActionKey = new HashMap<>();

  /** Reloads preferences whenever their values change. */
  private final OnSharedPreferenceChangeListener sharedPreferenceChangeListener =
      (prefs, key) -> {
        loadGestureIdToActionKeyMap();
        if (context.getResources().getString(R.string.pref_gesture_set_key).equals(key)) {
          currentGestureSet =
              SharedPreferencesUtils.getIntFromStringPref(
                  prefs,
                  context.getResources(),
                  R.string.pref_gesture_set_key,
                  R.string.pref_gesture_set_value_default);
        } else if (context
            .getResources()
            .getString(R.string.pref_multiple_gesture_set_key)
            .equals(key)) {
          gestureSetEnabled =
              isGestureSetEnabled(
                  context,
                  prefs,
                  R.string.pref_multiple_gesture_set_key,
                  R.bool.pref_multiple_gesture_set_default);
          updatePerAppGestureOverride();
        } else if (context
            .getResources()
            .getString(R.string.pref_per_app_gesture_set_enabled_key)
            .equals(key)) {
          perAppGestureSetEnabled =
              SharedPreferencesUtils.getBooleanPref(
                  prefs,
                  context.getResources(),
                  R.string.pref_per_app_gesture_set_enabled_key,
                  R.bool.pref_per_app_gesture_set_enabled_default);
          updatePerAppGestureOverride();
        } else if (key != null
            && key.startsWith(context.getString(R.string.pref_per_app_gesture_set_prefix))) {
          updatePerAppGestureOverride();
        } else if (context.getResources().getString(R.string.pref_invert_swipe_gestures_key)
            .equals(key)) {
          invertSwipeGestures =
              SharedPreferencesUtils.getBooleanPref(
                  prefs,
                  context.getResources(),
                  R.string.pref_invert_swipe_gestures_key,
                  R.bool.pref_invert_swipe_gestures_default);
        } else if (context
            .getResources()
            .getString(R.string.pref_multi_part_gestures_enabled_key)
            .equals(key)) {
          multiPartGesturesEnabled =
              SharedPreferencesUtils.getBooleanPref(
                  prefs,
                  context.getResources(),
                  R.string.pref_multi_part_gestures_enabled_key,
                  R.bool.pref_multi_part_gestures_enabled_default);
          loadGestureIdToActionKeyMap();
        }
      };

  public GestureShortcutMapping(Context context) {
    this.context = context;
    actionGestureUnsupported = context.getString(R.string.shortcut_value_unsupported);
    actionUnassigned = context.getString(R.string.shortcut_value_unassigned);
    actionTalkbackContextMenu = context.getString(R.string.shortcut_value_talkback_breakout);
    actionNextReadingMenuSetting = context.getString(R.string.shortcut_value_select_next_setting);
    actionReadingMenuUp =
        context.getString(R.string.shortcut_value_selected_setting_previous_action);
    actionReadingMenuDown = context.getString(R.string.shortcut_value_selected_setting_next_action);
    actionShortcut = context.getString(R.string.shortcut_value_show_custom_actions);
    nextWindowShortcut = context.getString(R.string.shortcut_value_next_window);
    mediaControlShortcut = context.getString(R.string.shortcut_value_media_control);
    prefs = SharedPreferencesUtils.getSharedPreferences(context);
    prefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    applyIosGestureSetDefaultsIfNeeded();
    loadGestureIdToActionKeyMap();
    gestureSetEnabled =
        isGestureSetEnabled(
            context,
            prefs,
            R.string.pref_multiple_gesture_set_key,
            R.bool.pref_multiple_gesture_set_default);
    perAppGestureSetEnabled =
        SharedPreferencesUtils.getBooleanPref(
            prefs,
            context.getResources(),
            R.string.pref_per_app_gesture_set_enabled_key,
            R.bool.pref_per_app_gesture_set_enabled_default);
    invertSwipeGestures =
        SharedPreferencesUtils.getBooleanPref(
            prefs,
            context.getResources(),
            R.string.pref_invert_swipe_gestures_key,
            R.bool.pref_invert_swipe_gestures_default);
    multiPartGesturesEnabled =
        SharedPreferencesUtils.getBooleanPref(
            prefs,
            context.getResources(),
            R.string.pref_multi_part_gestures_enabled_key,
            R.bool.pref_multi_part_gestures_enabled_default);
    currentGestureSet =
        gestureSetEnabled
            ? SharedPreferencesUtils.getIntFromStringPref(
                prefs,
                context.getResources(),
                R.string.pref_gesture_set_key,
                R.string.pref_gesture_set_value_default)
            : 0;
    updatePerAppGestureOverride();
  }

  private void applyIosGestureSetDefaultsIfNeeded() {
    if (!FeatureFlagReader.useMultipleGestureSet(context)) {
      return;
    }
    final int iosGestureSet = 1;
    SharedPreferences.Editor editor = prefs.edit();

    applyDefaultIfAbsent(
        editor, R.string.pref_shortcut_right_key, R.string.shortcut_value_next, iosGestureSet);
    applyDefaultIfAbsent(
        editor, R.string.pref_shortcut_left_key, R.string.shortcut_value_previous, iosGestureSet);
    applyDefaultIfAbsent(
        editor, R.string.pref_shortcut_left_and_right_key, R.string.shortcut_value_back, iosGestureSet);
    applyDefaultIfAbsent(
        editor, R.string.pref_shortcut_right_and_left_key, R.string.shortcut_value_back, iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_1finger_2tap_key,
        R.string.shortcut_value_perform_click_action,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_1finger_2tap_hold_key,
        R.string.shortcut_value_pass_through_next_gesture,
        iosGestureSet);

    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_2finger_swipe_right_key,
        R.string.shortcut_value_next_container,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_2finger_swipe_left_key,
        R.string.shortcut_value_prev_container,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_2finger_swipe_up_key,
        R.string.shortcut_value_read_from_top,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_2finger_swipe_down_key,
        R.string.shortcut_value_read_from_current,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_2finger_1tap_key,
        R.string.shortcut_value_pause_or_resume_feedback,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_2finger_2tap_key,
        R.string.shortcut_value_media_control,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_2finger_2tap_hold_key,
        R.string.shortcut_value_talkback_breakout,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_2finger_3tap_key,
        R.string.shortcut_value_item_chooser,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_2finger_3tap_hold_key,
        R.string.shortcut_value_quick_settings,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_2finger_rotate_cw_key,
        R.string.shortcut_value_select_next_setting,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_2finger_rotate_ccw_key,
        R.string.shortcut_value_select_previous_setting,
        iosGestureSet);

    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_3finger_1tap_key,
        R.string.shortcut_value_announce_item_position,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_3finger_1tap_hold_key,
        R.string.shortcut_value_split_tap_activate,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_3finger_swipe_up_key,
        R.string.shortcut_value_scroll_down,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_3finger_swipe_down_key,
        R.string.shortcut_value_scroll_up,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_3finger_swipe_left_key,
        R.string.shortcut_value_scroll_right,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_3finger_swipe_right_key,
        R.string.shortcut_value_scroll_left,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_3finger_2tap_key,
        R.string.shortcut_value_toggle_voice_feedback,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_3finger_3tap_key,
        R.string.shortcut_value_show_hide_screen,
        iosGestureSet);

    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_4finger_1tap_key,
        R.string.shortcut_value_first_in_screen,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_4finger_2tap_key,
        R.string.shortcut_value_last_in_screen,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_4finger_swipe_up_key,
        R.string.shortcut_value_first_in_screen,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_4finger_swipe_down_key,
        R.string.shortcut_value_last_in_screen,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_4finger_2tap_hold_key,
        R.string.shortcut_value_quick_settings,
        iosGestureSet);
    applyDefaultIfAbsent(
        editor,
        R.string.pref_shortcut_4finger_3tap_key,
        R.string.shortcut_value_live_recognition,
        iosGestureSet);

    editor.apply();
  }

  private void applyDefaultIfAbsent(
      SharedPreferences.Editor editor, int keyResId, int actionResId, int gestureSet) {
    String key = getPrefKeyWithGestureSet(context.getString(keyResId), gestureSet);
    if (!prefs.contains(key)) {
      editor.putString(key, context.getString(actionResId));
    }
  }

  public void onConfigurationChanged(Configuration newConfig) {
    if (newConfig != null && newConfig.screenLayout != previousScreenLayout) {
      loadGestureIdToActionKeyMap();
      previousScreenLayout = newConfig.screenLayout;
    }
  }

  public void onUnbind() {
    prefs.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
  }

  public int switchGestureSet(boolean isNext) {
    gestureSetEnabled =
        isGestureSetEnabled(
            context,
            prefs,
            R.string.pref_multiple_gesture_set_key,
            R.bool.pref_multiple_gesture_set_default);

    if (!gestureSetEnabled) {
      currentGestureSet = 0;
      return 0;
    }
    int gestureSet =
        SharedPreferencesUtils.getIntFromStringPref(
            prefs,
            context.getResources(),
            R.string.pref_gesture_set_key,
            R.string.pref_gesture_set_value_default);
    gestureSet =
        isNext
            ? (gestureSet + 1) % NUMBER_OF_GESTURE_SET
            : (gestureSet == 0) ? (NUMBER_OF_GESTURE_SET - 1) : (gestureSet - 1);
    SharedPreferencesUtils.putStringPref(
        prefs, context.getResources(), R.string.pref_gesture_set_key, String.valueOf(gestureSet));
    return gestureSet;
  }

  /** Returns gesture shortcut name for talkback context menu. */
  @Override
  @Nullable
  public CharSequence nodeMenuShortcut() {
    return getGestureFromActionKey(actionTalkbackContextMenu);
  }

  @Override
  @Nullable
  public CharSequence readingMenuNextSettingShortcut() {
    return getGestureFromActionKey(actionNextReadingMenuSetting);
  }

  @Override
  @Nullable
  public CharSequence readingMenuDownShortcut() {
    return getGestureFromActionKey(actionReadingMenuDown);
  }

  @Override
  @Nullable
  public CharSequence readingMenuUpShortcut() {
    return getGestureFromActionKey(actionReadingMenuUp);
  }

  @Override
  @Nullable
  public CharSequence actionsShortcut() {
    return getGestureFromActionKey(actionShortcut);
  }

  @Override
  @Nullable
  public CharSequence nextWindowShortcut() {
    return getGestureFromActionKey(nextWindowShortcut);
  }

  @Override
  @Nullable
  public CharSequence mediaControlShortcut() {
    return getGestureFromActionKey(mediaControlShortcut);
  }

  /**
   * Gets corresponding action from gesture-action mappings.
   *
   * @param gestureId The gesture id corresponds to the action
   * @return action key string
   */
  public String getActionKeyFromGestureId(int gestureId) {
    int resolvedGestureId = maybeSwapGestureId(gestureId);
    return getActionKeyFromGestureId(getActiveGestureSet(), resolvedGestureId);
  }

  private String getActionKeyFromGestureId(int index, int gestureId) {
    if (index < 0 || index >= NUMBER_OF_GESTURE_SET) {
      // Uses index 0 as a fallback.
      LogUtils.w(TAG, "Gesture set is not allowed; fallback to 0.");
      index = 0;
    }
    if (gestureId == GESTURE_TAP_HOLD_AND_2ND_FINGER_FORWARD_DOUBLE_TAP
        || gestureId == GESTURE_TAP_HOLD_AND_2ND_FINGER_BACKWARD_DOUBLE_TAP) {
      // These 2 gestures are dedicated for switching gesture set.
      return gestureSetEnabled ? context.getString(R.string.switch_gesture_set) : actionUnassigned;
    }
    String action = gestureIdToActionKey.get(index).get(gestureId);
    return action == null ? actionUnassigned : action;
  }

  /** Returns {@code true} if this gesture is supported. */
  public boolean isSupportedGesture(int gestureId) {
    String action = gestureIdToActionKey.get(0).get(gestureId);
    return action != null && !TextUtils.equals(action, actionGestureUnsupported);
  }

  /**
   * Gets corresponding action from fingerprint gesture-action mappings.
   *
   * @param fingerprintGestureId The fingerprint gesture id corresponds to the action
   * @return action key string
   */
  public String getActionKeyFromFingerprintGestureId(int fingerprintGestureId) {
    String action = fingerprintGestureIdToActionKey.get(fingerprintGestureId);
    return action == null ? actionUnassigned : action;
  }

  /**
   * Gets the highest priority gesture text for given action, including fingerprint gestures.
   *
   * <p><b>Priority:</b> 1. Default multi-finger gesture. 2. Default single-finger gesture. 3. User
   * customized multi-finger gesture. 4. User customized single-finger gesture. 5. Fingerprint
   * gesture.
   *
   * @param action The corresponding action assigned to the gesture
   * @return gesture text, or null if no gesture assigned to the action
   */
  @Nullable
  public String getGestureFromActionKey(String action) {
    int activeGestureSet = getActiveGestureSet();
    if (TextUtils.isEmpty(action) || !actionToGesture.get(activeGestureSet).containsKey(action)) {
      return null;
    }

    GestureCollector gestureCollector = actionToGesture.get(activeGestureSet).get(action);
    TalkBackGesture gesture = gestureCollector.getPrioritizedGesture();
    if (gesture == null) {
      LogUtils.w(
          TAG, "The Action is loaded in the mapping table, but no suitable gesture be found.");
      return null;
    }

    if (gesture.gestureType == FINGERPRINT) {
      return getFingerprintGestureString(context, gesture.gestureId);
    }

    return getGestureString(context, gesture.gestureId);
  }

  /**
   * Gets the gesture text from {@link #getGestureFromActionKey(String)} for each action.
   *
   * @param actions The corresponding actions assigned to the gesture
   * @return a list of gesture texts
   */
  @NonNull
  public List<String> getGestureTextsFromActionKeys(String... actions) {
    List<String> matchedGestures = new ArrayList<>();

    for (String action : actions) {
      String gestureString = getGestureFromActionKey(action);
      if (!TextUtils.isEmpty(gestureString)) {
        matchedGestures.add(gestureString);
      }
    }
    return matchedGestures;
  }

  /** Updates active package name for per-app gesture set. */
  public void setActivePackageName(@Nullable String packageName) {
    if (TextUtils.equals(currentPackageName, packageName)) {
      return;
    }
    currentPackageName = packageName;
    GestureSchemeStore.applyLinkedSchemeIfNeeded(context, prefs, currentPackageName);
    updatePerAppGestureOverride();
    loadGestureIdToActionKeyMap();
  }

  private int getActiveGestureSet() {
    if (currentPackageGestureSetOverride != null) {
      return currentPackageGestureSetOverride;
    }
    return currentGestureSet;
  }

  private void updatePerAppGestureOverride() {
    if (!gestureSetEnabled
        || !perAppGestureSetEnabled
        || TextUtils.isEmpty(currentPackageName)) {
      currentPackageGestureSetOverride = null;
      return;
    }
    int appGestureSet =
        PerAppGestureSetUtils.getGestureSetForPackage(prefs, context, currentPackageName);
    if (appGestureSet < 0 || appGestureSet >= NUMBER_OF_GESTURE_SET) {
      currentPackageGestureSetOverride = null;
    } else {
      currentPackageGestureSetOverride = appGestureSet;
    }
  }

  private String getScopedGestureKey(String baseKey) {
    if (!perAppGestureSetEnabled || TextUtils.isEmpty(currentPackageName)) {
      return baseKey;
    }
    String prefix = context.getString(R.string.pref_gesture_scope_pkg_prefix);
    return prefix + currentPackageName + ":" + baseKey;
  }

  private int maybeSwapGestureId(int gestureId) {
    if (!invertSwipeGestures) {
      return gestureId;
    }
    switch (gestureId) {
      case AccessibilityService.GESTURE_SWIPE_LEFT:
        return AccessibilityService.GESTURE_SWIPE_RIGHT;
      case AccessibilityService.GESTURE_SWIPE_RIGHT:
        return AccessibilityService.GESTURE_SWIPE_LEFT;
      case AccessibilityService.GESTURE_SWIPE_LEFT_AND_RIGHT:
        return AccessibilityService.GESTURE_SWIPE_RIGHT_AND_LEFT;
      case AccessibilityService.GESTURE_SWIPE_RIGHT_AND_LEFT:
        return AccessibilityService.GESTURE_SWIPE_LEFT_AND_RIGHT;
      default:
        return gestureId;
    }
  }

  /**
   * Returns an action-gesture mapping including all actions. The map key is an action key. The map
   * value is the text of the gesture which is assigned to the action.
   */
  public HashMap<String, String> getAllGestureTexts() {
    final HashMap<String, String> actionKeyToGestureText = new HashMap<>();
    actionToGesture
        .get(currentGestureSet)
        .forEach(
            (action, gestureCollector) -> {
              TalkBackGesture gesture = gestureCollector.getPrioritizedGesture();
              if (gesture == null) {
                return;
              }

              if (gesture.gestureType == FINGERPRINT) {
                actionKeyToGestureText.put(
                    action, getFingerprintGestureString(context, gesture.gestureId));
              } else {
                actionKeyToGestureText.put(action, getGestureString(context, gesture.gestureId));
              }
            });
    return actionKeyToGestureText;
  }

  /** Loads gesture-action mappings from shared preference. */
  private void loadGestureIdToActionKeyMap() {
    loadGestureIdToActionKeyMap(
        FeatureSupport.isMultiFingerGestureSupported(),
        FeatureSupport.isFingerprintGestureSupported(context));
  }

  /** Loads gesture-action mappings with multi-finger gesture on. It's only for testing purpose. */
  @VisibleForTesting
  protected void loadGestureIdToActionKeyMapWithMultiFingerGesture() {
    loadGestureIdToActionKeyMap(
        /* isMultiFingerOn= */ true, FeatureSupport.isFingerprintGestureSupported(context));
  }

  private void loadGestureIdToActionKeyMap(boolean isMultiFingerOn, boolean isFingerprintOn) {
    LogUtils.d(
        TAG,
        "loadActionToGestureIdMap - isMultiFingerOn : "
            + isMultiFingerOn
            + " isFingerprintOn : "
            + isFingerprintOn);

    // We initialize the list in the first run and later only update its elements.
    boolean isFirstRun = actionToGesture.isEmpty();

    HashMap<Integer, String> newFingerPrintGestureIdToActionKey = new HashMap<>();
    for (int index = 0; index < NUMBER_OF_GESTURE_SET; index++) {
      HashMap<Integer, String> gestureIdToActionKeyMap = new HashMap<>();
      HashMap<String, GestureCollector> actionToGestureMap = new HashMap<>();
      // Load TalkBack gestures.
      for (TalkBackGesture gesture : TalkBackGesture.values()) {
        // For some gestures, we have different behavior if the device is RTL. Skip the value of
        // non-RTL if it's RTL, and vice versa.
        if (skipGestureForRTL(gesture)) {
          continue;
        }

        // Skip multi-finger gestures when isMultiFingerOn = false.
        if (!isMultiFingerOn && gesture.gestureType == MULTI_FINGER) {
          continue;
        }

        if (!multiPartGesturesEnabled && isMultiPartGesture(gesture)) {
          continue;
        }

        // Skip fingerprint gestures when isFingerprintOn = false.
        if (!isFingerprintOn && gesture.gestureType == FINGERPRINT) {
          continue;
        }

        String keyId =
            getPrefKeyWithGestureSet(
                getScopedGestureKey(context.getString(gesture.keyId)), index);
        int defaultActionId = getDefaultActionIdForGestureSet(gesture, index);
        String action =
            prefs.getString(
                keyId,
                prefs.getString(
                    getPrefKeyWithGestureSet(context.getString(gesture.keyId), index),
                    context.getString(defaultActionId)));
        // When diagnosis-mode is on, override a gesture to dump node-tree to logs.
        if ((gesture == TalkBackGesture.FOUR_FINGER_SINGLE_TAP)
            && PreferencesActivityUtils.isDiagnosisModeOn(prefs, context.getResources())) {
          action = context.getString(R.string.shortcut_value_print_node_tree);
        }

        GestureCollector gestureCollector;
        if (actionToGestureMap.containsKey(action)) {
          gestureCollector = actionToGestureMap.get(action);
        } else {
          gestureCollector = new GestureCollector();
        }

        // Check the action is default or customized action.
        if (TextUtils.equals(action, context.getString(gesture.defaultActionId))) {
          gestureCollector.addDefaultGesture(gesture);
        } else {
          gestureCollector.addCustomizedGesture(gesture);
        }

        actionToGestureMap.put(action, gestureCollector);

        // Load the mapping table of the gesture id to the action.
        if (gesture.gestureType == FINGERPRINT) {
          // Fingerprint gestures use another gesture id system.
          newFingerPrintGestureIdToActionKey.put(
              gesture.gestureId,
              prefs.getString(
                  getScopedGestureKey(context.getString(gesture.keyId)),
                  prefs.getString(
                      context.getString(gesture.keyId),
                      context.getString(gesture.defaultActionId))));
        } else {
          gestureIdToActionKeyMap.put(gesture.gestureId, action);
        }
      }

      // Non-customizable shortcut for SPLIT_TYPE
      gestureIdToActionKeyMap.put(
          GESTURE_FAKED_SPLIT_TYPING, context.getString(R.string.shortcut_value_split_typing));
      // Don't need to keep unassigned action in the map.
      actionToGestureMap.remove(actionUnassigned);

      if (isFirstRun) {
        gestureIdToActionKey.add(gestureIdToActionKeyMap);
        actionToGesture.add(actionToGestureMap);
      } else {
        gestureIdToActionKey.set(index, gestureIdToActionKeyMap);
        actionToGesture.set(index, actionToGestureMap);
      }
    }
    fingerprintGestureIdToActionKey = newFingerPrintGestureIdToActionKey;
  }

  private int getDefaultActionIdForGestureSet(TalkBackGesture gesture, int index) {
    if (index == GESTURE_SET_IOS) {
      Integer override = IOS_DEFAULT_ACTIONS.get(gesture);
      if (override != null) {
        return override;
      }
    }
    return gesture.defaultActionId;
  }

  private boolean isMultiPartGesture(TalkBackGesture gesture) {
    switch (gesture.gestureId) {
      case AccessibilityService.GESTURE_SWIPE_UP_AND_DOWN:
      case AccessibilityService.GESTURE_SWIPE_DOWN_AND_UP:
      case AccessibilityService.GESTURE_SWIPE_LEFT_AND_RIGHT:
      case AccessibilityService.GESTURE_SWIPE_RIGHT_AND_LEFT:
      case AccessibilityService.GESTURE_SWIPE_UP_AND_LEFT:
      case AccessibilityService.GESTURE_SWIPE_UP_AND_RIGHT:
      case AccessibilityService.GESTURE_SWIPE_DOWN_AND_LEFT:
      case AccessibilityService.GESTURE_SWIPE_DOWN_AND_RIGHT:
      case AccessibilityService.GESTURE_SWIPE_LEFT_AND_UP:
      case AccessibilityService.GESTURE_SWIPE_LEFT_AND_DOWN:
      case AccessibilityService.GESTURE_SWIPE_RIGHT_AND_UP:
      case AccessibilityService.GESTURE_SWIPE_RIGHT_AND_DOWN:
        return true;
      default:
        return false;
    }
  }

  private boolean skipGestureForRTL(TalkBackGesture gesture) {
    if (gesture.rtlType == RTL_UNRELATED) {
      return false;
    }

    if (WindowUtils.isScreenLayoutRTL(context)) {
      // Skip LTR gestures.
      if (gesture.rtlType == LTR_GESTURE) {
        return true;
      }
    } else {
      // Skip RTL gestures.
      if (gesture.rtlType == RTL_GESTURE) {
        return true;
      }
    }

    return false;
  }

  public void dump(Logger dumpLogger) {
    dumpLogger.log("Gesture mapping");
    for (Map.Entry<Integer, String> entry : gestureIdToActionKey.get(0).entrySet()) {
      dumpLogger.log(
          "Gesture = %s, action = %s", getGestureString(context, entry.getKey()), entry.getValue());
    }
    dumpLogger.log("");
  }

  /** Returns the corresponding action resource Id of action key. */
  public static String getActionString(Context context, String actionKeyString) {
    if (TextUtils.isEmpty(actionKeyString)) {
      return context.getString(R.string.shortcut_unassigned);
    }

    final android.content.res.Resources res = context.getResources();
    final int configHash = res.getConfiguration().hashCode();
    Map<String, String> cache;
    synchronized (ACTION_CACHE_LOCK) {
      if (ACTION_CACHE_CONFIG_HASH != configHash) {
        ACTION_STRING_CACHE.clear();
        ACTION_CACHE_CONFIG_HASH = configHash;
      }
      cache = ACTION_STRING_CACHE.get(res);
      if (cache == null) {
        cache = buildActionStringCache(res);
        ACTION_STRING_CACHE.put(res, cache);
      }
    }
    String cached = cache.get(actionKeyString);
    return cached != null ? cached : context.getString(R.string.shortcut_unassigned);
  }

  private static Map<String, String> buildActionStringCache(android.content.res.Resources res) {
    HashMap<String, String> map = new HashMap<>();
    for (TalkbackAction action : TalkbackAction.values()) {
      if (action.actionKeyResId != -1) {
        map.put(res.getString(action.actionKeyResId), res.getString(action.actionNameResId));
      }
    }
    return map;
  }

  /** Returns all supported action keys. */
  public static List<String> getAllActionKeys(Context context) {
    ArrayList<String> keys = new ArrayList<>();
    for (TalkbackAction action : TalkbackAction.values()) {
      if (action == TalkbackAction.UNASSIGNED_ACTION) {
        continue;
      }
      if (action.actionKeyResId == -1) {
        continue;
      }
      String key = context.getString(action.actionKeyResId);
      if (!TextUtils.isEmpty(key)) {
        keys.add(key);
      }
    }
    return keys;
  }

  /** Returns if the device supports multiple gesture set. */
  public static boolean isGestureSetEnabled(
      Context context, SharedPreferences prefs, int resKeyId, int defaultValue) {
    return FeatureSupport.supportMultipleGestureSet()
        && FeatureFlagReader.useMultipleGestureSet(context)
        && SharedPreferencesUtils.getBooleanPref(
            prefs, context.getResources(), resKeyId, defaultValue);
  }

  /** Returns derived preference key which is affixed with gesture set. */
  public static String getPrefKeyWithGestureSet(String key, int gestureSet) {
    if (gestureSet < 0 || gestureSet >= NUMBER_OF_GESTURE_SET) {
      gestureSet = 0;
    }
    String derivedKey = key;
    int splitIndex = key.indexOf("-");
    if (gestureSet == 0) {
      if (splitIndex != -1) {
        derivedKey = key.substring(0, splitIndex);
      }
    } else {
      if (splitIndex == -1) {
        derivedKey = key + "-" + gestureSet;
      } else {
        derivedKey = key.substring(0, splitIndex + 1) + gestureSet;
      }
    }
    return derivedKey;
  }

  /** Returns the corresponding TalkBack action null when undefined. */
  @Nullable
  public TalkbackAction getActionEvent(String actionKeyString) {
    for (TalkbackAction action : TalkbackAction.values()) {
      if (action.actionKeyResId != -1
          && TextUtils.equals(context.getString(action.actionKeyResId), actionKeyString)) {
        return action;
      }
    }
    return null;
  }

  /** Returns the corresponding gesture string of gesture id. */
  @Nullable
  public static String getGestureString(Context context, int gestureId) {
    switch (gestureId) {
      case AccessibilityService.GESTURE_SWIPE_UP:
        return context.getString(R.string.title_pref_shortcut_up);
      case AccessibilityService.GESTURE_SWIPE_DOWN:
        return context.getString(R.string.title_pref_shortcut_down);
      case AccessibilityService.GESTURE_SWIPE_LEFT:
        return context.getString(R.string.title_pref_shortcut_left);
      case AccessibilityService.GESTURE_SWIPE_RIGHT:
        return context.getString(R.string.title_pref_shortcut_right);
      case GESTURE_EDGE_SWIPE_RIGHT:
        return context.getString(R.string.title_pref_shortcut_edge_swipe_right);
      case GESTURE_EDGE_SWIPE_LEFT:
        return context.getString(R.string.title_pref_shortcut_edge_swipe_left);
      case AccessibilityService.GESTURE_SWIPE_UP_AND_DOWN:
        return context.getString(R.string.title_pref_shortcut_up_and_down);
      case AccessibilityService.GESTURE_SWIPE_DOWN_AND_UP:
        return context.getString(R.string.title_pref_shortcut_down_and_up);
      case AccessibilityService.GESTURE_SWIPE_LEFT_AND_RIGHT:
        return context.getString(R.string.title_pref_shortcut_left_and_right);
      case AccessibilityService.GESTURE_SWIPE_RIGHT_AND_LEFT:
        return context.getString(R.string.title_pref_shortcut_right_and_left);
      case AccessibilityService.GESTURE_SWIPE_UP_AND_RIGHT:
        return context.getString(R.string.title_pref_shortcut_up_and_right);
      case AccessibilityService.GESTURE_SWIPE_UP_AND_LEFT:
        return context.getString(R.string.title_pref_shortcut_up_and_left);
      case AccessibilityService.GESTURE_SWIPE_DOWN_AND_RIGHT:
        return context.getString(R.string.title_pref_shortcut_down_and_right);
      case AccessibilityService.GESTURE_SWIPE_DOWN_AND_LEFT:
        return context.getString(R.string.title_pref_shortcut_down_and_left);
      case AccessibilityService.GESTURE_SWIPE_RIGHT_AND_DOWN:
        return context.getString(R.string.title_pref_shortcut_right_and_down);
      case AccessibilityService.GESTURE_SWIPE_RIGHT_AND_UP:
        return context.getString(R.string.title_pref_shortcut_right_and_up);
      case AccessibilityService.GESTURE_SWIPE_LEFT_AND_DOWN:
        return context.getString(R.string.title_pref_shortcut_left_and_down);
      case AccessibilityService.GESTURE_SWIPE_LEFT_AND_UP:
        return context.getString(R.string.title_pref_shortcut_left_and_up);
      case AccessibilityService.GESTURE_2_FINGER_SWIPE_UP:
        return context.getString(R.string.title_pref_shortcut_2finger_swipe_up);
      case AccessibilityService.GESTURE_2_FINGER_SWIPE_DOWN:
        return context.getString(R.string.title_pref_shortcut_2finger_swipe_down);
      case AccessibilityService.GESTURE_2_FINGER_SWIPE_LEFT:
        return context.getString(R.string.title_pref_shortcut_2finger_swipe_left);
      case AccessibilityService.GESTURE_2_FINGER_SWIPE_RIGHT:
        return context.getString(R.string.title_pref_shortcut_2finger_swipe_right);
      case GESTURE_ROTATE_CW:
        return context.getString(R.string.title_pref_shortcut_2finger_rotate_cw);
      case GESTURE_ROTATE_CCW:
        return context.getString(R.string.title_pref_shortcut_2finger_rotate_ccw);
      case AccessibilityService.GESTURE_2_FINGER_SINGLE_TAP:
        return context.getString(R.string.title_pref_shortcut_2finger_1tap);
      case AccessibilityService.GESTURE_2_FINGER_DOUBLE_TAP:
        return context.getString(R.string.title_pref_shortcut_2finger_2tap);
      case AccessibilityService.GESTURE_2_FINGER_TRIPLE_TAP:
        return context.getString(R.string.title_pref_shortcut_2finger_3tap);
      case AccessibilityService.GESTURE_3_FINGER_SWIPE_UP:
        return context.getString(R.string.title_pref_shortcut_3finger_swipe_up);
      case AccessibilityService.GESTURE_3_FINGER_SWIPE_DOWN:
        return context.getString(R.string.title_pref_shortcut_3finger_swipe_down);
      case AccessibilityService.GESTURE_3_FINGER_SWIPE_LEFT:
        return context.getString(R.string.title_pref_shortcut_3finger_swipe_left);
      case AccessibilityService.GESTURE_3_FINGER_SWIPE_RIGHT:
        return context.getString(R.string.title_pref_shortcut_3finger_swipe_right);
      case AccessibilityService.GESTURE_3_FINGER_SINGLE_TAP:
        return context.getString(R.string.title_pref_shortcut_3finger_1tap);
      case AccessibilityService.GESTURE_3_FINGER_DOUBLE_TAP:
        return context.getString(R.string.title_pref_shortcut_3finger_2tap);
      case AccessibilityService.GESTURE_3_FINGER_TRIPLE_TAP:
        return context.getString(R.string.title_pref_shortcut_3finger_3tap);
      case AccessibilityService.GESTURE_4_FINGER_SWIPE_UP:
        return context.getString(R.string.title_pref_shortcut_4finger_swipe_up);
      case AccessibilityService.GESTURE_4_FINGER_SWIPE_DOWN:
        return context.getString(R.string.title_pref_shortcut_4finger_swipe_down);
      case AccessibilityService.GESTURE_4_FINGER_SWIPE_LEFT:
        return context.getString(R.string.title_pref_shortcut_4finger_swipe_left);
      case AccessibilityService.GESTURE_4_FINGER_SWIPE_RIGHT:
        return context.getString(R.string.title_pref_shortcut_4finger_swipe_right);
      case AccessibilityService.GESTURE_4_FINGER_SINGLE_TAP:
        return context.getString(R.string.title_pref_shortcut_4finger_1tap);
      case AccessibilityService.GESTURE_4_FINGER_DOUBLE_TAP:
        return context.getString(R.string.title_pref_shortcut_4finger_2tap);
      case AccessibilityService.GESTURE_4_FINGER_TRIPLE_TAP:
        return context.getString(R.string.title_pref_shortcut_4finger_3tap);
      case AccessibilityService.GESTURE_2_FINGER_DOUBLE_TAP_AND_HOLD:
        return context.getString(R.string.title_pref_shortcut_2finger_2tap_hold);
      case AccessibilityService.GESTURE_3_FINGER_DOUBLE_TAP_AND_HOLD:
        return context.getString(R.string.title_pref_shortcut_3finger_2tap_hold);
      case AccessibilityService.GESTURE_4_FINGER_DOUBLE_TAP_AND_HOLD:
        return context.getString(R.string.title_pref_shortcut_4finger_2tap_hold);
      case GESTURE_TOUCH_EXPLORATION:
        return FeatureSupport.supportGestureMotionEvents()
            ? context.getString(R.string.gesture_name_touch_explore)
            : null;
      case GESTURE_PASSTHROUGH:
        return FeatureSupport.supportGestureMotionEvents()
            ? context.getString(R.string.gesture_name_pass_through)
            : null;
      case AccessibilityService.GESTURE_UNKNOWN:
        return FeatureSupport.supportGestureMotionEvents()
            ? context.getString(R.string.gesture_name_unknown)
            : null;
      case AccessibilityService.GESTURE_DOUBLE_TAP:
        return FeatureSupport.supportGestureMotionEvents()
            ? context.getString(R.string.gesture_name_double_tap)
            : null;
      case AccessibilityService.GESTURE_DOUBLE_TAP_AND_HOLD:
        return FeatureSupport.supportGestureMotionEvents()
            ? context.getString(R.string.gesture_name_double_tap_and_hold)
            : null;
      case AccessibilityService.GESTURE_2_FINGER_TRIPLE_TAP_AND_HOLD:
        return FeatureSupport.supportGestureMotionEvents()
            ? context.getString(R.string.gesture_name_2finger_3tap_hold)
            : null;
      case AccessibilityService.GESTURE_3_FINGER_SINGLE_TAP_AND_HOLD:
        return FeatureSupport.supportGestureMotionEvents()
            ? context.getString(R.string.gesture_name_3finger_tap_hold)
            : null;
      case AccessibilityService.GESTURE_3_FINGER_TRIPLE_TAP_AND_HOLD:
        return FeatureSupport.supportGestureMotionEvents()
            ? context.getString(R.string.gesture_name_3finger_3tap_hold)
            : null;
      case GESTURE_FAKED_SPLIT_TYPING:
        return context.getString(R.string.shortcut_value_split_typing);
      default:
        return null;
    }
  }

  @Nullable
  public static String getFingerprintGestureString(Context context, int fingerprintGestureId) {
    switch (fingerprintGestureId) {
      case FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_UP:
        return context.getString(R.string.title_pref_shortcut_fingerprint_up);
      case FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_DOWN:
        return context.getString(R.string.title_pref_shortcut_fingerprint_down);
      case FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_RIGHT:
        return context.getString(R.string.title_pref_shortcut_fingerprint_right);
      case FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_LEFT:
        return context.getString(R.string.title_pref_shortcut_fingerprint_left);
      default:
        return null;
    }
  }

  /** Keeps different kind of gestures for a TalkBack action, and prioritizes gestures. */
  private static class GestureCollector {
    List<TalkBackGesture> defaultGestures = new ArrayList<>();
    List<TalkBackGesture> customizedGestures = new ArrayList<>();

    void addDefaultGesture(TalkBackGesture gesture) {
      defaultGestures.add(gesture);
    }

    void addCustomizedGesture(TalkBackGesture gesture) {
      customizedGestures.add(gesture);
    }

    /**
     * Returns the gesture id with the highest priority, it will return null if no suitable gesture
     * in the collector.
     */
    @Nullable
    TalkBackGesture getPrioritizedGesture() {
      // Priority : default multi-finger gesture -> default single-finger gesture -> customized
      // multi-finger gesture -> customized single-finger gesture -> fingerprint.
      if (!defaultGestures.isEmpty()) {
        Collections.sort(defaultGestures, new GestureComparator());
        return defaultGestures.get(0);
      }

      if (!customizedGestures.isEmpty()) {
        Collections.sort(customizedGestures, new GestureComparator());
        return customizedGestures.get(0);
      }

      return null;
    }
  }

  /**
   * Comparator for {@link TalkBackGesture}. Will sort gestures by following order: Multi-finger
   * gesture > Single-finger gesture > Fingerprint.
   */
  private static class GestureComparator implements Comparator<TalkBackGesture> {
    @Override
    public int compare(TalkBackGesture g1, TalkBackGesture g2) {
      return Integer.compare(g1.gestureType, g2.gestureType);
    }
  }
}
