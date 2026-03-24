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

package com.google.android.accessibility.talkback.floating;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.ImageView;
import com.google.android.accessibility.talkback.R;

/** Controls a movable floating menu button. */
public final class FloatingMenuController {

  /** Callback for click actions. */
  public interface ActionHandler {
    void onFloatingMenuClicked();
  }

  private final Context context;
  private final WindowManager windowManager;
  private final ActionHandler actionHandler;
  private ImageView floatingView;
  private WindowManager.LayoutParams layoutParams;
  private boolean enabled;

  public FloatingMenuController(Context context, ActionHandler actionHandler) {
    this.context = context.getApplicationContext();
    this.actionHandler = actionHandler;
    this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
  }

  public void setEnabled(boolean enabled) {
    if (this.enabled == enabled) {
      return;
    }
    this.enabled = enabled;
    if (enabled) {
      show();
    } else {
      hide();
    }
  }

  public void shutdown() {
    enabled = false;
    hide();
  }

  private void show() {
    if (floatingView != null) {
      return;
    }

    final int sizePx = dpToPx(48);
    final int paddingPx = dpToPx(8);
    final int touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

    floatingView = new ImageView(context);
    floatingView.setImageResource(R.drawable.quantum_gm_ic_accessibility_new_vd_theme_24);
    floatingView.setContentDescription(
        context.getString(R.string.floating_menu_content_description));
    floatingView.setClickable(true);
    floatingView.setFocusable(false);
    floatingView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

    GradientDrawable background = new GradientDrawable();
    background.setColor(0xCC111111);
    background.setCornerRadius(dpToPx(24));
    floatingView.setBackground(background);

    layoutParams =
        new WindowManager.LayoutParams(
            sizePx + paddingPx * 2,
            sizePx + paddingPx * 2,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT);
    layoutParams.gravity = Gravity.TOP | Gravity.START;
    layoutParams.x = dpToPx(16);
    layoutParams.y = dpToPx(180);

    floatingView.setOnTouchListener(
        new View.OnTouchListener() {
          private float initialTouchX;
          private float initialTouchY;
          private int initialX;
          private int initialY;
          private boolean moved;

          @Override
          public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
              case MotionEvent.ACTION_DOWN:
                moved = false;
                initialX = layoutParams.x;
                initialY = layoutParams.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                return true;
              case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - initialTouchX;
                float dy = event.getRawY() - initialTouchY;
                if (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop) {
                  moved = true;
                  layoutParams.x = initialX + Math.round(dx);
                  layoutParams.y = initialY + Math.round(dy);
                  windowManager.updateViewLayout(floatingView, layoutParams);
                }
                return true;
              case MotionEvent.ACTION_UP:
                if (!moved && actionHandler != null) {
                  view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                  actionHandler.onFloatingMenuClicked();
                }
                return true;
              default:
                return false;
            }
          }
        });

    windowManager.addView(floatingView, layoutParams);
  }

  private void hide() {
    if (floatingView == null) {
      return;
    }
    try {
      windowManager.removeView(floatingView);
    } finally {
      floatingView = null;
      layoutParams = null;
    }
  }

  private int dpToPx(int dp) {
    return Math.round(
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
  }
}
