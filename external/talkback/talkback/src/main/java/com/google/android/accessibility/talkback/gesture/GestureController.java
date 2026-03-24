/*
 * Copyright (C) 2015 Google Inc.
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

import static android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS;
import static com.google.android.accessibility.talkback.Feedback.AdjustVolume.Action.DECREASE_VOLUME;
import static com.google.android.accessibility.talkback.Feedback.AdjustVolume.Action.INCREASE_VOLUME;
import static com.google.android.accessibility.talkback.Feedback.AdjustVolume.StreamType.STREAM_TYPE_ACCESSIBILITY;
import static com.google.android.accessibility.talkback.Feedback.BrailleDisplay.Action.TOGGLE_BRAILLE_DISPLAY_ON_OR_OFF;
import static com.google.android.accessibility.talkback.Feedback.ContinuousRead.Action.START_AT_CURSOR;
import static com.google.android.accessibility.talkback.Feedback.ContinuousRead.Action.START_AT_TOP;
import static com.google.android.accessibility.talkback.Feedback.DimScreen.Action.BRIGHTEN;
import static com.google.android.accessibility.talkback.Feedback.DimScreen.Action.DIM;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.COPY;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.CURSOR_TO_BEGINNING;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.CURSOR_TO_END;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.CUT;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.END_SELECT;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.PASTE;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.SELECT_ALL;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.START_SELECT;
import static com.google.android.accessibility.talkback.Feedback.Focus.Action.CLICK_CURRENT;
import static com.google.android.accessibility.talkback.Feedback.Focus.Action.LONG_CLICK_CURRENT;
import static com.google.android.accessibility.talkback.Feedback.FocusDirection.Action.NEXT_GRANULARITY;
import static com.google.android.accessibility.talkback.Feedback.FocusDirection.Action.NEXT_PAGE;
import static com.google.android.accessibility.talkback.Feedback.FocusDirection.Action.PREVIOUS_GRANULARITY;
import static com.google.android.accessibility.talkback.Feedback.FocusDirection.Action.PREVIOUS_PAGE;
import static com.google.android.accessibility.talkback.Feedback.FocusDirection.Action.SCROLL_DOWN;
import static com.google.android.accessibility.talkback.Feedback.FocusDirection.Action.SCROLL_LEFT;
import static com.google.android.accessibility.talkback.Feedback.FocusDirection.Action.SCROLL_RIGHT;
import static com.google.android.accessibility.talkback.Feedback.FocusDirection.Action.SCROLL_UP;
import static com.google.android.accessibility.talkback.Feedback.PassThroughMode.Action.PASSTHROUGH_CONFIRM_DIALOG;
import static com.google.android.accessibility.talkback.Feedback.Speech.Action.COPY_LAST;
import static com.google.android.accessibility.talkback.Feedback.Speech.Action.SAVE_LAST;
import static com.google.android.accessibility.talkback.Feedback.Speech.Action.TOGGLE_VOICE_FEEDBACK;
import static com.google.android.accessibility.talkback.Feedback.UniversalSearch.Action.TOGGLE_SEARCH;
import static com.google.android.accessibility.talkback.Feedback.VoiceRecognition.Action.START_LISTENING;
import static com.google.android.accessibility.talkback.actor.SystemActionPerformer.GLOBAL_ACTION_ACCESSIBILITY_BUTTON;
import static com.google.android.accessibility.talkback.actor.SystemActionPerformer.GLOBAL_ACTION_ACCESSIBILITY_BUTTON_CHOOSER;
import static com.google.android.accessibility.talkback.actor.SystemActionPerformer.GLOBAL_ACTION_KEYCODE_HEADSETHOOK;
import static com.google.android.accessibility.talkback.actor.TalkBackUIActor.Type.GESTURE_ACTION_OVERLAY;
import static com.google.android.accessibility.talkback.contextmenu.ListMenuManager.MenuId.CONTEXT;
import static com.google.android.accessibility.talkback.contextmenu.ListMenuManager.MenuId.CUSTOM_ACTION;
import static com.google.android.accessibility.talkback.contextmenu.ListMenuManager.MenuId.LANGUAGE;
import static com.google.android.accessibility.talkback.contextmenu.ListMenuManager.MenuId.CLIPBOARD_HISTORY;
import static com.google.android.accessibility.talkback.contextmenu.ListMenuManager.MenuId.QUICK_MENU;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.ANNOUNCE_REAL_ACTION;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.UNKNOWN_ANNOUNCEMENT;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;
import static com.google.android.accessibility.utils.input.CursorGranularity.DEFAULT;
import static com.google.android.accessibility.utils.monitor.InputModeTracker.INPUT_MODE_TOUCH;
import static com.google.android.accessibility.utils.output.SpeechController.QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_BACKWARD;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.FingerprintGestureController;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.ActorState;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Feedback.TalkBackUI;
import com.google.android.accessibility.talkback.Feedback.TriggerIntent.Action;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.actor.gemini.GeminiConfiguration;
import com.google.android.accessibility.talkback.actor.gemini.GeminiFunctionUtils;
import com.google.android.accessibility.talkback.actor.gemini.GeminiPrefs;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics;
import com.google.android.accessibility.talkback.contextmenu.ListMenuManager;
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor;
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping.TalkbackAction;
import com.google.android.accessibility.talkback.interpreters.AccessibilityFocusInterpreter;
import com.google.android.accessibility.talkback.plugin.PluginRegistry;
import com.google.android.accessibility.talkback.selector.SelectorController;
import com.google.android.accessibility.talkback.selector.SelectorController.AnnounceType;
import com.google.android.accessibility.talkback.selector.SelectorController.Setting;
import com.google.android.accessibility.talkback.trainingcommon.TrainingActivity;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.KeyboardUtils;
import com.google.android.accessibility.utils.NetworkUtils;
import com.google.android.accessibility.utils.Performance;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.StringBuilderUtils;
import com.google.android.accessibility.utils.TreeDebug;
import com.google.android.accessibility.utils.WindowUtils;
import com.google.android.accessibility.utils.monitor.ScreenMonitor;
import com.google.android.accessibility.utils.output.FeedbackItem;
import com.google.android.accessibility.utils.output.SpeechController;
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;
import com.google.android.accessibility.utils.screencapture.ScreenshotCapture;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Class to handle incoming gestures to TalkBack. Make sure tutorial still works TODO: Map
 * actions to ints, and store in a map that changes with prefs
 */
public class GestureController {

  private static final String LOG_TAG = "GestureController";
  private static final String BLINDROID_ASSIST_OBSTACLE =
      "com.screenreaders.blindroid.obstacle.ObstacleAssistActivity";
  private static final String BLINDROID_ASSIST_FACE =
      "com.screenreaders.blindroid.face.FaceAssistActivity";
  private static final String BLINDROID_ASSIST_CURRENCY =
      "com.screenreaders.blindroid.currency.CurrencyActivity";
  private static final String BLINDROID_ASSIST_DOCUMENT =
      "com.screenreaders.blindroid.document.DocumentAssistActivity";
  private static final String BLINDROID_ASSIST_SCAN_HUB =
      "com.screenreaders.blindroid.scan.ScanHubActivity";

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Member variables

  private final TalkBackService service;
  private final Pipeline.FeedbackReturner pipeline;
  private final ActorState actorState;
  private final ListMenuManager menuManager;
  private final SelectorController selectorController;
  private final AccessibilityFocusMonitor accessibilityFocusMonitor;
  private final AccessibilityFocusInterpreter accessibilityFocusInterpreter;
  private final GestureShortcutMapping gestureShortcutMapping;
  private final TalkBackAnalytics analytics;

  private final @NonNull Map<Integer, Integer> captureGestureIdToAnnouncements = new HashMap<>();
  private final @NonNull Map<Integer, Integer> captureFingerprintGestureIdToAnnouncements =
      new HashMap<>();

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Constructor methods

  public GestureController(
      TalkBackService service,
      Pipeline.FeedbackReturner pipeline,
      ActorState actorState,
      ListMenuManager menuManager,
      SelectorController selectorController,
      AccessibilityFocusMonitor accessibilityFocusMonitor,
      AccessibilityFocusInterpreter accessibilityFocusInterpreter,
      GestureShortcutMapping gestureShortcutMapping,
      TalkBackAnalytics analytics) {
    if (pipeline == null) {
      throw new IllegalStateException();
    }
    if (menuManager == null) {
      throw new IllegalStateException();
    }
    if (selectorController == null) {
      throw new IllegalStateException();
    }

    this.pipeline = pipeline;
    this.actorState = actorState;
    this.menuManager = menuManager;
    this.service = service;
    this.selectorController = selectorController;
    this.accessibilityFocusMonitor = accessibilityFocusMonitor;
    this.accessibilityFocusInterpreter = accessibilityFocusInterpreter;
    this.gestureShortcutMapping = gestureShortcutMapping;
    this.analytics = analytics;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Screen-swipe-shortcut methods

  /**
   * Maps fingerprint gesture Id to TalkBack action.
   *
   * @param gesture Fingerprint gesture Id
   * @return Mapped action shortcut
   */
  private String actionFromFingerprintGesture(int gesture) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(service);
    switch (gesture) {
      case FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_UP:
        return prefs.getString(
            service.getString(R.string.pref_shortcut_fingerprint_up_key),
            service.getString(R.string.pref_shortcut_fingerprint_up_default));
      case FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_DOWN:
        return prefs.getString(
            service.getString(R.string.pref_shortcut_fingerprint_down_key),
            service.getString(R.string.pref_shortcut_fingerprint_down_default));
      case FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_LEFT:
        return prefs.getString(
            service.getString(R.string.pref_shortcut_fingerprint_left_key),
            service.getString(R.string.pref_shortcut_fingerprint_left_default));
      case FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_RIGHT:
        return prefs.getString(
            service.getString(R.string.pref_shortcut_fingerprint_right_key),
            service.getString(R.string.pref_shortcut_fingerprint_right_default));
      default:
        return service.getString(R.string.shortcut_value_unassigned);
    }
  }

  public void performAction(String action, EventId eventId) {
    maybeInterruptAllFeedback(action);
    boolean result = true;
    if (action.equals(service.getString(R.string.switch_gesture_set))) {
      speak(
          service.getString(
              R.string.switch_gesture_set_to,
              gestureShortcutMapping.switchGestureSet(/* isNext= */ true)));
    } else if (action.equals(service.getString(R.string.shortcut_value_split_typing))) {
      accessibilityFocusInterpreter.performSplitTap(EVENT_ID_UNTRACKED);
    } else if (action.equals(service.getString(R.string.shortcut_value_unassigned))) {
      // Do Nothing
    } else if (PluginRegistry.isPluginActionKey(action)) {
      result = PluginRegistry.launchAction(service, action);
    } else if (action.equals(service.getString(R.string.shortcut_value_previous))) {
      result =
          pipeline.returnFeedback(
              eventId,
              Feedback.focusDirection(SEARCH_FOCUS_BACKWARD)
                  // Sets granularity to default because "Previous item" action always moves at
                  // default granularity.
                  .setGranularity(DEFAULT)
                  .setInputMode(INPUT_MODE_TOUCH)
                  .setDefaultToInputFocus(true)
                  .setScroll(true)
                  .setWrap(true));
    } else if (action.equals(service.getString(R.string.shortcut_value_next))) {
      result =
          pipeline.returnFeedback(
              eventId,
              Feedback.focusDirection(SEARCH_FOCUS_FORWARD)
                  // Sets granularity to default because "Next item" action always moves at default
                  // granularity.
                  .setGranularity(DEFAULT)
                  .setInputMode(INPUT_MODE_TOUCH)
                  .setDefaultToInputFocus(true)
                  .setScroll(true)
                  .setWrap(true));
    } else if (action.equals(service.getString(R.string.shortcut_value_scroll_back))) {
      result = pipeline.returnFeedback(eventId, Feedback.focusDirection(PREVIOUS_PAGE));
    } else if (action.equals(service.getString(R.string.shortcut_value_scroll_forward))) {
      result = pipeline.returnFeedback(eventId, Feedback.focusDirection(NEXT_PAGE));
    } else if (action.equals(service.getString(R.string.shortcut_value_scroll_up))) {
      result = pipeline.returnFeedback(eventId, Feedback.focusDirection(SCROLL_UP));
    } else if (action.equals(service.getString(R.string.shortcut_value_scroll_down))) {
      result = pipeline.returnFeedback(eventId, Feedback.focusDirection(SCROLL_DOWN));
    } else if (action.equals(service.getString(R.string.shortcut_value_scroll_left))) {
      result = pipeline.returnFeedback(eventId, Feedback.focusDirection(SCROLL_LEFT));
    } else if (action.equals(service.getString(R.string.shortcut_value_scroll_right))) {
      result = pipeline.returnFeedback(eventId, Feedback.focusDirection(SCROLL_RIGHT));
    } else if (action.equals(service.getString(R.string.shortcut_value_first_in_screen))) {
      result = pipeline.returnFeedback(eventId, Feedback.focusTop(INPUT_MODE_TOUCH));
    } else if (action.equals(service.getString(R.string.shortcut_value_last_in_screen))) {
      result = pipeline.returnFeedback(eventId, Feedback.focusBottom(INPUT_MODE_TOUCH));
    } else if (action.equals(service.getString(R.string.shortcut_value_media_control))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(GLOBAL_ACTION_KEYCODE_HEADSETHOOK));
    } else if (action.equals(service.getString(R.string.shortcut_value_increase_volume))) {
      changeAccessibilityVolume(eventId, /* decrease= */ false);
    } else if (action.equals(service.getString(R.string.shortcut_value_decrease_volume))) {
      changeAccessibilityVolume(eventId, /* decrease= */ true);
    } else if (action.equals(service.getString(R.string.shortcut_value_back))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(AccessibilityService.GLOBAL_ACTION_BACK));
    } else if (action.equals(service.getString(R.string.shortcut_value_home))) {
      // Dismiss TalkBack menu
      menuManager.dismissAll();
      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(AccessibilityService.GLOBAL_ACTION_HOME));
    } else if (action.equals(service.getString(R.string.shortcut_value_voice_commands))) {
      if (ScreenMonitor.isDeviceLocked(service)) {
        speak(
            service.getString(
                R.string.voice_command_screen_locked_hint,
                gestureShortcutMapping.nodeMenuShortcut()));
      } else {
        pipeline.returnFeedback(eventId, Feedback.speech(SAVE_LAST));
        result =
            pipeline.returnFeedback(
                eventId, Feedback.voiceRecognition(START_LISTENING, /* checkDialog= */ true));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_overview))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(AccessibilityService.GLOBAL_ACTION_RECENTS));
    } else if (action.equals(service.getString(R.string.shortcut_value_notifications))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS));
    } else if (action.equals(service.getString(R.string.shortcut_value_quick_settings))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS));
    } else if (action.equals(service.getString(R.string.shortcut_value_all_apps))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS));
    } else if (action.equals(service.getString(R.string.shortcut_value_talkback_breakout))) {
      result = menuManager.showMenu(CONTEXT, eventId);
    } else if (action.equals(service.getString(R.string.shortcut_value_local_breakout))) {
      result = menuManager.showMenu(CONTEXT, eventId);
    } else if (action.equals(service.getString(R.string.shortcut_value_quick_menu))) {
      result = menuManager.showMenu(QUICK_MENU, eventId, R.string.quick_menu_empty);
    } else if (action.equals(service.getString(R.string.shortcut_value_clipboard_history))) {
      result = menuManager.showMenu(CLIPBOARD_HISTORY, eventId, R.string.clipboard_history_empty);
    } else if (action.equals(service.getString(R.string.shortcut_value_gesture_scheme_manager))) {
      pipeline.returnFeedback(
          eventId, Feedback.triggerIntent(Feedback.TriggerIntent.Action.TRIGGER_GESTURE_SCHEME_MANAGER));
    } else if (action.equals(service.getString(R.string.shortcut_value_backup_settings))) {
      pipeline.returnFeedback(
          eventId, Feedback.triggerIntent(Feedback.TriggerIntent.Action.TRIGGER_BACKUP_SETTINGS));
    } else if (action.equals(
        service.getString(R.string.shortcut_value_toggle_per_app_gesture_set))) {
      result = togglePerAppGestureSet(eventId);
    } else if (action.equals(service.getString(R.string.shortcut_value_show_custom_actions))) {
      result = menuManager.showMenu(CUSTOM_ACTION, eventId);
    } else if (action.equals(service.getString(R.string.shortcut_value_editing))) {
      // Combines editing menu and custom action menu. If user set the gesture to editing menu, it
      // will launch custom action menu.
      result = menuManager.showMenu(CUSTOM_ACTION, eventId);
    } else if (action.equals(service.getString(R.string.shortcut_value_show_language_options))) {
      result = menuManager.showMenu(LANGUAGE, eventId);
    } else if (action.equals(service.getString(R.string.shortcut_value_previous_granularity))) {
      result = pipeline.returnFeedback(eventId, Feedback.focusDirection(PREVIOUS_GRANULARITY));
    } else if (action.equals(service.getString(R.string.shortcut_value_next_granularity))) {
      result = pipeline.returnFeedback(eventId, Feedback.focusDirection(NEXT_GRANULARITY));
    } else if (action.equals(service.getString(R.string.shortcut_value_previous_window))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.previousWindow(INPUT_MODE_TOUCH).setDefaultToInputFocus(true));
    } else if (action.equals(service.getString(R.string.shortcut_value_next_window))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.nextWindow(INPUT_MODE_TOUCH).setDefaultToInputFocus(true));
    } else if (action.equals(service.getString(R.string.shortcut_value_prev_container))) {
      result = pipeline.returnFeedback(eventId, Feedback.prevContainer(INPUT_MODE_TOUCH));
    } else if (action.equals(service.getString(R.string.shortcut_value_next_container))) {
      result = pipeline.returnFeedback(eventId, Feedback.nextContainer(INPUT_MODE_TOUCH));
    } else if (action.equals(service.getString(R.string.shortcut_value_read_from_top))) {
      result = pipeline.returnFeedback(eventId, Feedback.continuousRead(START_AT_TOP));
    } else if (action.equals(service.getString(R.string.shortcut_value_read_from_current))) {
      result = pipeline.returnFeedback(eventId, Feedback.continuousRead(START_AT_CURSOR));
    } else if (action.equals(service.getString(R.string.shortcut_value_print_node_tree))) {
      TreeDebug.logNodeTreesOnAllDisplays(service);
      pipeline.returnFeedback(
          eventId, Feedback.speech(service.getString(R.string.dump_node_tree_description)));
    } else if (action.equals(service.getString(R.string.shortcut_value_print_performance_stats))) {
      Performance.getInstance().displayLabelToStats();
      Performance.getInstance().displayStatToLabelCompare();
      Performance.getInstance().displayAllEventStats();
    } else if (action.equals(service.getString(R.string.shortcut_value_perform_click_action))) {
      result = pipeline.returnFeedback(eventId, Feedback.focus(CLICK_CURRENT));
    } else if (action.equals(
        service.getString(R.string.shortcut_value_perform_long_click_action))) {
      result = pipeline.returnFeedback(eventId, Feedback.focus(LONG_CLICK_CURRENT));
    } else if (action.equals(service.getString(R.string.shortcut_value_select_previous_setting))) {
      selectorController.selectPreviousOrNextSetting(
          eventId, AnnounceType.DESCRIPTION_AND_HINT, false);
    } else if (action.equals(service.getString(R.string.shortcut_value_select_next_setting))) {
      selectorController.selectPreviousOrNextSetting(
          eventId, AnnounceType.DESCRIPTION_AND_HINT, true);
    } else if (action.equals(
        service.getString(R.string.shortcut_value_selected_setting_previous_action))) {
      selectorController.adjustSelectedSetting(eventId, false);
    } else if (action.equals(
        service.getString(R.string.shortcut_value_selected_setting_next_action))) {
      selectorController.adjustSelectedSetting(eventId, true);
    } else if (action.equals(
        service.getString(R.string.shortcut_value_action_setting_activate_current_action))) {
      if (SelectorController.getCurrentSetting(service).equals(Setting.ACTIONS)) {
        selectorController.activateCurrentAction(eventId);
      } else {
        result = pipeline.returnFeedback(eventId, Feedback.focus(CLICK_CURRENT));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_screen_search))) {
      result = pipeline.returnFeedback(eventId, Feedback.universalSearch(TOGGLE_SEARCH));
    } else if (action.equals(service.getString(R.string.shortcut_value_item_chooser))) {
      result = pipeline.returnFeedback(eventId, Feedback.universalSearch(TOGGLE_SEARCH));
    } else if (action.equals(service.getString(R.string.shortcut_value_live_recognition))) {
      result = toggleLiveRecognition();
    } else if (action.equals(service.getString(R.string.shortcut_value_show_hide_screen))) {
      if (actorState.getDimScreen().isDimmingEnabled()) {
        result = pipeline.returnFeedback(eventId, Feedback.dimScreen(BRIGHTEN));
      } else {
        result = pipeline.returnFeedback(eventId, Feedback.dimScreen(DIM));
      }
    } else if (action.equals(
        service.getString(R.string.shortcut_value_pass_through_next_gesture))) {
      result =
          pipeline.returnFeedback(eventId, Feedback.passThroughMode(PASSTHROUGH_CONFIRM_DIALOG));
    } else if (action.equals(service.getString(R.string.shortcut_value_a11y_button))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(GLOBAL_ACTION_ACCESSIBILITY_BUTTON));
    } else if (action.equals(service.getString(R.string.shortcut_value_a11y_button_long_press))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.systemAction(GLOBAL_ACTION_ACCESSIBILITY_BUTTON_CHOOSER));
    } else if (action.equals(service.getString(R.string.shortcut_value_pause_or_resume_feedback))) {
      pipeline.returnFeedback(eventId, Feedback.speech(Feedback.Speech.Action.PAUSE_OR_RESUME));
    } else if (action.equals(service.getString(R.string.shortcut_value_start_selection_mode))) {
      AccessibilityNodeInfoCompat node = getSelectTextFocus();
      if (node == null) {
        result = false;
      } else {
        // Editable or non-editable selectable text found.
        if (actorState.getDirectionNavigation().isSelectionModeActive()) {
          result = pipeline.returnFeedback(eventId, Feedback.edit(node, END_SELECT));
        } else {
          result = pipeline.returnFeedback(eventId, Feedback.edit(node, START_SELECT));
        }
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_move_cursor_to_beginning))) {
      AccessibilityNodeInfoCompat node = getEditTextFocus();
      if (node == null) {
        result = false;
      } else {
        pipeline.returnFeedback(eventId, Feedback.edit(node, CURSOR_TO_BEGINNING));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_move_cursor_to_end))) {
      AccessibilityNodeInfoCompat node = getEditTextFocus();
      if (node == null) {
        result = false;
      } else {
        pipeline.returnFeedback(eventId, Feedback.edit(node, CURSOR_TO_END));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_select_all))) {
      AccessibilityNodeInfoCompat node = getSelectTextFocus();
      if (node == null) {
        result = false;
      } else {
        pipeline.returnFeedback(eventId, Feedback.edit(node, SELECT_ALL));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_copy))) {
      AccessibilityNodeInfoCompat node =
          accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ true);
      result = pipeline.returnFeedback(eventId, Feedback.edit(node, COPY));
    } else if (action.equals(service.getString(R.string.shortcut_value_cut))) {
      AccessibilityNodeInfoCompat node = getEditTextFocus();
      if (node == null) {
        result = false;
      } else {
        // Edit text found.
        result = pipeline.returnFeedback(eventId, Feedback.edit(node, CUT));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_paste))) {
      AccessibilityNodeInfoCompat node = getEditTextFocus();
      if (node == null) {
        result = false;
      } else {
        // Edit text found.
        result = pipeline.returnFeedback(eventId, Feedback.edit(node, PASTE));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_voice_feedback))) {
      pipeline.returnFeedback(eventId, Feedback.speech(TOGGLE_VOICE_FEEDBACK));
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_explore_by_touch))) {
      result =
          toggleBooleanPreference(
              eventId,
              R.string.pref_explore_by_touch_key,
              R.bool.pref_explore_by_touch_default,
              R.string.explore_by_touch_state);
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_single_tap))) {
      result =
          toggleBooleanPreference(
              eventId,
              R.string.pref_single_tap_key,
              R.bool.pref_single_tap_default,
              R.string.single_tap_state);
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_web_scripts))) {
      result =
          toggleBooleanPreference(
              eventId,
              R.string.pref_web_scripts_key,
              R.bool.pref_web_scripts_default,
              R.string.web_scripts_state);
    } else if (action.equals(
        service.getString(R.string.shortcut_value_toggle_speak_notifications))) {
      result =
          toggleBooleanPreference(
              eventId,
              R.string.pref_speak_notifications_key,
              R.bool.pref_speak_notifications_default,
              R.string.speak_notifications_state);
    } else if (action.equals(service.getString(R.string.shortcut_value_macro_1))) {
      result = performGestureMacro(eventId, 1);
    } else if (action.equals(service.getString(R.string.shortcut_value_macro_2))) {
      result = performGestureMacro(eventId, 2);
    } else if (action.equals(service.getString(R.string.shortcut_value_macro_3))) {
      result = performGestureMacro(eventId, 3);
    } else if (action.equals(service.getString(R.string.shortcut_value_document_scan))) {
      result =
          launchBlindroidAssist(BLINDROID_ASSIST_DOCUMENT)
              || performImageRecognition(/* preferDetailed= */ true);
    } else if (action.equals(service.getString(R.string.shortcut_value_read_screen))) {
      result = performReadScreenAi(eventId);
    } else if (action.equals(service.getString(R.string.shortcut_value_scan_hub))) {
      result = launchBlindroidAssist(BLINDROID_ASSIST_SCAN_HUB);
    } else if (action.equals(service.getString(R.string.shortcut_value_announce_item_position))) {
      AccessibilityNodeInfoCompat node =
          accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ true);
      CharSequence info = service.getGlobalVariables().getCollectionItemTransitionDescription(node);
      if (TextUtils.isEmpty(info)) {
        info = service.getGlobalVariables().getCollectionTransitionDescription();
      }
      if (TextUtils.isEmpty(info)) {
        speak(service.getString(R.string.announce_item_position_unavailable));
        result = false;
      } else {
        result = pipeline.returnFeedback(eventId, Feedback.speech(info));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_announce_time))) {
      String timeText = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(new java.util.Date());
      speak(service.getString(R.string.announce_time_template, timeText));
    } else if (action.equals(service.getString(R.string.shortcut_value_announce_battery))) {
      Integer percent = getBatteryPercent();
      if (percent == null) {
        speak(service.getString(R.string.announce_battery_unavailable));
      } else {
        speak(service.getString(R.string.announce_battery_template, percent));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_repeat_last_utterance))) {
      pipeline.returnFeedback(
          eventId, Feedback.part().setSpeech(Feedback.Speech.create(REPEAT_SAVED)));
    } else if (action.equals(service.getString(R.string.shortcut_value_spell_last_utterance))) {
      pipeline.returnFeedback(
          eventId, Feedback.part().setSpeech(Feedback.Speech.create(SPELL_SAVED)));
    } else if (action.equals(service.getString(R.string.shortcut_value_copy_last_spoken_phrase))) {
      pipeline.returnFeedback(
          eventId, Feedback.part().setSpeech(Feedback.Speech.create(COPY_LAST)));
    } else if (action.equals(service.getString(R.string.shortcut_value_braille_keyboard))) {
      String inputMethodInfoId = KeyboardUtils.getEnabledImeId(service, service.getPackageName());
      if (!TextUtils.isEmpty(inputMethodInfoId)) {
        result = service.getSoftKeyboardController().switchToInputMethod(inputMethodInfoId);
      } else {
        result =
            pipeline.returnFeedback(
                eventId,
                Feedback.speech(
                    service.getString(R.string.switch_to_braille_keyboard_failure_msg)));
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_braille_display_settings))) {
      pipeline.returnFeedback(
          eventId, Feedback.triggerIntent(Action.TRIGGER_BRAILLE_DISPLAY_SETTINGS));
    } else if (action.equals(service.getString(R.string.shortcut_value_tutorial))) {
      pipeline.returnFeedback(eventId, Feedback.triggerIntent(Action.TRIGGER_TUTORIAL));
    } else if (action.equals(service.getString(R.string.shortcut_value_practice_gestures))) {
      pipeline.returnFeedback(eventId, Feedback.triggerIntent(Action.TRIGGER_PRACTICE_GESTURE));
    } else if (action.equals(service.getString(R.string.shortcut_value_report_gesture))) {
      result = pipeline.returnFeedback(eventId, Feedback.reportGesture());
    } else if (action.equals(service.getString(R.string.shortcut_value_toggle_braille_display))) {
      result =
          pipeline.returnFeedback(
              eventId, Feedback.performBrailleDisplayAction(TOGGLE_BRAILLE_DISPLAY_ON_OR_OFF));
    } else if (action.equals(service.getString(R.string.shortcut_value_describe_image))) {
      AccessibilityNodeInfoCompat node =
          accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
      if (node == null) {
        speak(service.getString(R.string.image_caption_no_result));
      } else {
        Feedback.Part.Builder feedback =
            GeminiFunctionUtils.getPreferredImageDescriptionFeedback(service, actorState, node);
        result = feedback != null && pipeline.returnFeedback(EVENT_ID_UNTRACKED, feedback);
      }
    } else if (action.equals(service.getString(R.string.shortcut_value_translate_text))) {
      result = performTranslateAction();
    } else if (action.equals(service.getString(R.string.shortcut_value_object_recognition))) {
      result =
          launchBlindroidAssist(BLINDROID_ASSIST_OBSTACLE)
              || performImageRecognition(/* preferDetailed= */ true);
    } else if (action.equals(service.getString(R.string.shortcut_value_face_recognition))) {
      result =
          launchBlindroidAssist(BLINDROID_ASSIST_FACE)
              || performImageRecognition(/* preferDetailed= */ true);
    } else if (action.equals(service.getString(R.string.shortcut_value_money_recognition))) {
      result =
          launchBlindroidAssist(BLINDROID_ASSIST_CURRENCY)
              || performImageRecognition(/* preferDetailed= */ true);
    } else if (action.equals(service.getString(R.string.shortcut_value_scene_recognition))) {
      result =
          launchBlindroidAssist(BLINDROID_ASSIST_OBSTACLE)
              || performImageRecognition(/* preferDetailed= */ true);
    }

    if (!result) {
      // Show failure hint if perform action fail.
      pipeline.returnFeedback(eventId, Feedback.sound(R.raw.complete));
    }
  }

  public boolean isFingerprintGestureAssigned(int fingerprintGestureId) {
    return !TextUtils.equals(
        service.getString(R.string.shortcut_value_unassigned),
        actionFromFingerprintGesture(fingerprintGestureId));
  }

  public void onGesture(int gestureId, EventId eventId) {
    if (gestureHandledByTraining(gestureId, /* isFingerprintGesture= */ false)) {
      return;
    }

    String action = gestureShortcutMapping.getActionKeyFromGestureId(gestureId);
    TalkbackAction actionEvent = gestureShortcutMapping.getActionEvent(action);
    if (actionEvent != null) {
      analytics.onShortcutActionEvent(actionEvent);
    }
    LogUtils.v(
        LOG_TAG,
        "Recognized gesture id: %d [%s] , action: [%s]",
        gestureId,
        GestureShortcutMapping.getGestureString(service, gestureId),
        action);
    performAction(action, eventId);
  }

  public void setCaptureGestureIdToAnnouncements(
      @NonNull ImmutableMap<Integer, Integer> captureGestureIdToAnnouncements,
      @NonNull ImmutableMap<Integer, Integer> captureFingerprintGestureIdToAnnouncements) {
    this.captureGestureIdToAnnouncements.clear();
    if (!captureGestureIdToAnnouncements.isEmpty()) {
      this.captureGestureIdToAnnouncements.putAll(captureGestureIdToAnnouncements);
    }

    this.captureFingerprintGestureIdToAnnouncements.clear();
    if (!captureFingerprintGestureIdToAnnouncements.isEmpty()) {
      this.captureFingerprintGestureIdToAnnouncements.putAll(
          captureFingerprintGestureIdToAnnouncements);
    }
  }

  private boolean gestureHandledByTraining(int gestureId, boolean isFingerprintGesture) {
    if (!isOnTrainingPage()) {
      return false;
    }
    // TalkBack can ignore the gesture, which is handled by OnGestureListener.onCaptureGesture(), if
    // captured gesture list exists and the current window is training.
    @Nullable Integer feedbackResId =
        isFingerprintGesture
            ? captureFingerprintGestureIdToAnnouncements.get(gestureId)
            : captureGestureIdToAnnouncements.get(gestureId);
    if (feedbackResId == null || feedbackResId == UNKNOWN_ANNOUNCEMENT) {
      return false;
    }

    String feedbackString;
    if (feedbackResId == ANNOUNCE_REAL_ACTION) {
      SpannableStringBuilder gestureAndAction = new SpannableStringBuilder();
      if (isFingerprintGesture) {
        StringBuilderUtils.appendWithSeparator(
            gestureAndAction,
            GestureShortcutMapping.getFingerprintGestureString(service, gestureId),
            GestureShortcutMapping.getActionString(
                service, gestureShortcutMapping.getActionKeyFromFingerprintGestureId(gestureId)));
      } else {
        StringBuilderUtils.appendWithSeparator(
            gestureAndAction,
            GestureShortcutMapping.getGestureString(service, gestureId),
            GestureShortcutMapping.getActionString(
                service, gestureShortcutMapping.getActionKeyFromGestureId(gestureId)));
      }
      feedbackString = gestureAndAction.toString();
    } else {
      feedbackString = service.getString(feedbackResId);
    }
    pipeline.returnFeedback(
        /* eventId= */ null,
        Feedback.speech(
            feedbackString,
            SpeakOptions.create().setQueueMode(QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH)));
    return true;
  }

  /** Returns true if the active window is training activity. */
  private boolean isOnTrainingPage() {
    // Compares resource ID with all child nodes on the active window if navigate-up arrow is
    // shown because the first child node is the navigate-up arrow which is no resource ID.
    return WindowUtils.rootChildMatchesResId(service, TrainingActivity.ROOT_RES_ID);
  }

  /**
   * The interruptAllFeedback inside performAction is effective only when CRM is active.. Here to
   * perform an equivalent logic(by interruptGentle) to interrupt feedback when CRM is inactive.
   */
  public void onFingerprintGesture(int fingerprintGestureId, EventId eventId) {
    if (gestureHandledByTraining(fingerprintGestureId, /* isFingerprintGesture= */ true)) {
      return;
    }
    pipeline.returnFeedback(eventId, Feedback.part().setInterruptGentle(true));
    String action = actionFromFingerprintGesture(fingerprintGestureId);
    TalkbackAction actionEvent = gestureShortcutMapping.getActionEvent(action);
    if (actionEvent != null) {
      analytics.onShortcutActionEvent(actionEvent);
    }
    LogUtils.v(
        LOG_TAG,
        "Recognized fingerprint gesture id: %d [%s] , action: [%s]",
        fingerprintGestureId,
        GestureShortcutMapping.getFingerprintGestureString(service, fingerprintGestureId),
        action);
    performAction(action, eventId);
  }

  /**
   * Change the action if it's not allowed in continuous reading mode.
   *
   * @param action the action mapping from preference
   */
  private void maybeInterruptAllFeedback(String action) {
    if (!actorState.getContinuousRead().isActive()) {
      return;
    }

    if (action.equals(service.getString(R.string.shortcut_value_previous))
        || action.equals(service.getString(R.string.shortcut_value_next))
        || action.equals(service.getString(R.string.shortcut_value_unassigned))) {
      return;
    }
    service.interruptAllFeedback(false);
  }

  /** Checks if users have changed gestures manually. */
  public static boolean isAnyGestureChanged(Context context) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String[] gestureKeys = context.getResources().getStringArray(R.array.pref_shortcut_keys);
    for (String key : gestureKeys) {
      if (prefs.contains(key)) {
        return true;
      }
    }
    return false;
  }

  private @Nullable AccessibilityNodeInfoCompat getSelectTextFocus() {
    @Nullable AccessibilityNodeInfoCompat node =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ true);
    if (AccessibilityNodeInfoUtils.isTextSelectable(node)) {
      return node;
    }
    node = accessibilityFocusMonitor.getEditingNodeFromFocusedKeyboard(node);
    if (node != null) {
      return node;
    } else {
      speak(service.getString(R.string.not_selectable));
      return null;
    }
  }

  private boolean performTranslateAction() {
    @Nullable AccessibilityNodeInfoCompat node =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ true);
    CharSequence text = AccessibilityNodeInfoUtils.getSelectedNodeText(node);
    if (TextUtils.isEmpty(text)) {
      text = AccessibilityNodeInfoUtils.getNodeText(node);
    }
    if (TextUtils.isEmpty(text)) {
      speak(service.getString(R.string.translate_no_text));
      return false;
    }

    Intent intent = new Intent(Intent.ACTION_PROCESS_TEXT);
    intent.setType("text/plain");
    intent.putExtra(Intent.EXTRA_PROCESS_TEXT, text);
    intent.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    if (intent.resolveActivity(service.getPackageManager()) == null) {
      speak(service.getString(R.string.translate_no_app));
      return false;
    }

    service.startActivity(intent);
    return true;
  }

  private boolean performImageRecognition(boolean preferDetailed) {
    AccessibilityNodeInfoCompat node =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    if (node == null) {
      speak(service.getString(R.string.image_caption_no_result));
      return false;
    }
    Feedback.Part.Builder feedback =
        GeminiFunctionUtils.getPreferredImageDescriptionFeedback(service, actorState, node);
    if (feedback != null && preferDetailed) {
      return pipeline.returnFeedback(EVENT_ID_UNTRACKED, feedback);
    }
    return pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.performImageCaptions(node, true));
  }

  private boolean performReadScreenAi(EventId eventId) {
    if (!SharedPreferencesUtils.getBooleanPref(
        SharedPreferencesUtils.getSharedPreferences(service),
        service.getResources(),
        R.string.pref_ai_read_screen_enabled_key,
        R.bool.pref_ai_read_screen_enabled_default)) {
      speak(service.getString(R.string.read_screen_ai_disabled));
      return false;
    }
    boolean performanceEnabled = Performance.getInstance().getComputeStatsEnabled();
    long startMs = SystemClock.elapsedRealtime();
    if (!GeminiConfiguration.isGeminiVoiceCommandEnabled(service)
        || !GeminiPrefs.hasApiKey(service)
        || !NetworkUtils.isNetworkConnected(service)) {
      return performReadScreenOffline(eventId);
    }
    String prompt = GeminiConfiguration.getReadScreenPrompt(service);
    if (GeminiConfiguration.isReadScreenHapticEnabled(service)) {
      pipeline.returnFeedback(
          eventId,
          Feedback.vibration(com.google.android.accessibility.utils.R.array.read_screen_pattern));
    }
    int maxDimension = service.getResources().getInteger(R.integer.blindreader_ai_screenshot_max_dim);
    ScreenshotCapture.takeScreenshot(
        service,
        (screenCapture, isFormatSupported) -> {
          if (performanceEnabled) {
            long durationMs = SystemClock.elapsedRealtime() - startMs;
            LogUtils.i(
                LOG_TAG,
                "Read screen AI screenshot latency=%dms (formatSupported=%s)",
                durationMs,
                isFormatSupported);
          }
          if (!isFormatSupported) {
            performReadScreenOffline(eventId);
            return;
          }
          pipeline.returnFeedback(
              eventId, Feedback.geminiRequest(/* requestId= */ -1, prompt, screenCapture));
        },
        maxDimension);
    return true;
  }

  private boolean performReadScreenOffline(EventId eventId) {
    pipeline.returnFeedback(
        eventId, Feedback.speech(service.getString(R.string.read_screen_ai_unavailable_fallback)));
    pipeline.returnFeedback(eventId, Feedback.continuousRead(START_AT_TOP));
    return true;
  }

  private boolean launchBlindroidAssist(String className) {
    String packageName = getPackageNameForClass(className);
    if (TextUtils.isEmpty(packageName)) {
      return false;
    }
    Intent intent = new Intent();
    intent.setComponent(new ComponentName(packageName, className));
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if (intent.resolveActivity(service.getPackageManager()) == null) {
      // Fall back to in-service recognition when Blindroid scanner is unavailable.
      return false;
    }
    service.startActivity(intent);
    return true;
  }

  private String getPackageNameForClass(String className) {
    int lastDot = className.lastIndexOf('.');
    if (lastDot <= 0) {
      return className;
    }
    return className.substring(0, lastDot);
  }

  private @Nullable AccessibilityNodeInfoCompat getEditTextFocus() {
    @Nullable AccessibilityNodeInfoCompat node =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ true);
    if (Role.getRole(node) == Role.ROLE_EDIT_TEXT) {
      return node;
    } else {
      speak(service.getString(R.string.not_editable));
      return null;
    }
  }

  /** voice feedback function */
  private void speak(String text) {
    SpeakOptions speakOptions =
        SpeakOptions.create()
            .setFlags(
                FeedbackItem.FLAG_NO_HISTORY
                    | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE
                    | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_MICROPHONE_ACTIVE
                    | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_SSB_ACTIVE);
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(text, speakOptions));
  }

  private boolean performGestureMacro(EventId eventId, int macroIndex) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(service);
    boolean enabled =
        SharedPreferencesUtils.getBooleanPref(
            prefs, service.getResources(), R.string.pref_macro_enabled_key, R.bool.pref_macro_enabled_default);
    if (!enabled) {
      speak(service.getString(R.string.macro_disabled));
      return false;
    }

    String name = getMacroName(prefs, macroIndex);
    String unassigned = service.getString(R.string.shortcut_value_unassigned);
    java.util.List<String> actions = GestureMacroStore.getActionList(service, macroIndex);
    int count = 0;
    for (String action : actions) {
      if (TextUtils.isEmpty(action) || TextUtils.equals(action, unassigned)) {
        continue;
      }
      if (isMacroAction(action)) {
        continue;
      }
      count++;
    }
    if (count == 0) {
      speak(service.getString(R.string.macro_empty, name));
      return false;
    }

    speak(service.getString(R.string.macro_running, name));
    for (String action : actions) {
      if (TextUtils.isEmpty(action) || TextUtils.equals(action, unassigned)) {
        continue;
      }
      if (isMacroAction(action)) {
        continue;
      }
      performAction(action, eventId);
    }
    return true;
  }

  private String getMacroName(SharedPreferences prefs, int macroIndex) {
    int keyResId;
    int defaultResId;
    switch (macroIndex) {
      case 1:
        keyResId = R.string.pref_macro_1_name_key;
        defaultResId = R.string.macro_default_name_1;
        break;
      case 2:
        keyResId = R.string.pref_macro_2_name_key;
        defaultResId = R.string.macro_default_name_2;
        break;
      case 3:
      default:
        keyResId = R.string.pref_macro_3_name_key;
        defaultResId = R.string.macro_default_name_3;
        break;
    }
    String value = prefs.getString(service.getString(keyResId), null);
    if (TextUtils.isEmpty(value)) {
      value = service.getString(defaultResId);
    }
    return value;
  }

  private boolean isMacroAction(String action) {
    return TextUtils.equals(action, service.getString(R.string.shortcut_value_macro_1))
        || TextUtils.equals(action, service.getString(R.string.shortcut_value_macro_2))
        || TextUtils.equals(action, service.getString(R.string.shortcut_value_macro_3));
  }

  private void changeAccessibilityVolume(EventId eventId, boolean decrease) {
    boolean result =
        pipeline.returnFeedback(
            eventId,
            Feedback.adjustVolume(
                decrease ? DECREASE_VOLUME : INCREASE_VOLUME, STREAM_TYPE_ACCESSIBILITY));
    String volumeText;
    if (result) {
      volumeText =
          service.getString(
              decrease
                  ? R.string.template_volume_change_decrease
                  : R.string.template_volume_change_increase);
    } else {
      volumeText =
          service.getString(
              decrease
                  ? R.string.template_volume_change_minimum
                  : R.string.template_volume_change_maximum);
    }

    pipeline.returnFeedback(
        eventId,
        Feedback.speech(
            volumeText,
            SpeakOptions.create()
                .setQueueMode(SpeechController.QUEUE_MODE_INTERRUPT)
                .setFlags(
                    FeedbackItem.FLAG_NO_HISTORY
                        | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE
                        | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_MICROPHONE_ACTIVE
                        | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_SSB_ACTIVE
                        | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_PHONE_CALL_ACTIVE
                        | FeedbackItem.FLAG_SKIP_DUPLICATE)));

    pipeline.returnFeedback(
        eventId,
        Feedback.talkBackUI(
            TalkBackUI.Action.SHOW_GESTURE_ACTION_UI,
            GESTURE_ACTION_OVERLAY,
            volumeText,
            /* showIcon= */ false));
  }

  private boolean toggleBooleanPreference(
      EventId eventId, int keyResId, int defaultResId, int announcementResId) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(service);
    boolean newValue =
        !SharedPreferencesUtils.getBooleanPref(
            prefs, service.getResources(), keyResId, defaultResId);
    SharedPreferencesUtils.putBooleanPref(prefs, service.getResources(), keyResId, newValue);
    String announcement =
        service.getString(
            announcementResId,
            newValue ? service.getString(R.string.value_on) : service.getString(R.string.value_off));
    if (!TextUtils.isEmpty(announcement)) {
      pipeline.returnFeedback(
          eventId,
          Feedback.speech(
              announcement,
              SpeakOptions.create()
                  .setFlags(
                      FeedbackItem.FLAG_NO_HISTORY
                          | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE
                          | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_MICROPHONE_ACTIVE
                          | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_SSB_ACTIVE
                          | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_PHONE_CALL_ACTIVE
                          | FeedbackItem.FLAG_SKIP_DUPLICATE)));
    }
    return true;
  }

  private boolean toggleLiveRecognition() {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(service);
    boolean imageEnabled =
        SharedPreferencesUtils.getBooleanPref(
            prefs,
            service.getResources(),
            R.string.pref_auto_image_description_key,
            R.bool.pref_auto_image_description_default);
    boolean textEnabled =
        SharedPreferencesUtils.getBooleanPref(
            prefs,
            service.getResources(),
            R.string.pref_auto_text_recognition_key,
            R.bool.pref_auto_text_recognition_default);
    boolean newValue = !(imageEnabled || textEnabled);
    SharedPreferencesUtils.putBooleanPref(
        prefs, service.getResources(), R.string.pref_auto_image_description_key, newValue);
    SharedPreferencesUtils.putBooleanPref(
        prefs, service.getResources(), R.string.pref_auto_text_recognition_key, newValue);
    speak(
        service.getString(
            newValue ? R.string.live_recognition_enabled : R.string.live_recognition_disabled));
    return true;
  }

  @Nullable
  private Integer getBatteryPercent() {
    Intent batteryStatus =
        service.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    if (batteryStatus == null) {
      return null;
    }
    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
    if (level < 0 || scale <= 0) {
      return null;
    }
    return Math.round(level * 100f / scale);
  }

  private boolean togglePerAppGestureSet(EventId eventId) {
    if (service.getRootInActiveWindow() == null
        || service.getRootInActiveWindow().getPackageName() == null) {
      pipeline.returnFeedback(
          eventId, Feedback.speech(service.getString(R.string.per_app_gesture_set_unavailable)));
      return false;
    }
    String packageName = service.getRootInActiveWindow().getPackageName().toString();
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(service);
    int globalSet =
        SharedPreferencesUtils.getIntFromStringPref(
            prefs,
            service.getResources(),
            R.string.pref_gesture_set_key,
            R.string.pref_gesture_set_value_default);
    int currentSet = PerAppGestureSetUtils.getGestureSetForPackage(prefs, service, packageName);
    int nextSet;
    if (currentSet == PerAppGestureSetUtils.SET_GLOBAL) {
      nextSet =
          (globalSet == PerAppGestureSetUtils.SET_IOS)
              ? PerAppGestureSetUtils.SET_DEFAULT
              : PerAppGestureSetUtils.SET_IOS;
    } else {
      nextSet =
          (currentSet == PerAppGestureSetUtils.SET_IOS)
              ? PerAppGestureSetUtils.SET_DEFAULT
              : PerAppGestureSetUtils.SET_IOS;
    }
    PerAppGestureSetUtils.setGestureSetForPackage(prefs, service, packageName, nextSet);
    String[] entries = service.getResources().getStringArray(R.array.pref_gesture_set_entries);
    String label = (nextSet >= 0 && nextSet < entries.length) ? entries[nextSet] : String.valueOf(nextSet);
    pipeline.returnFeedback(
        eventId,
        Feedback.speech(service.getString(R.string.per_app_gesture_set_switched, label)));
    return true;
  }
}
