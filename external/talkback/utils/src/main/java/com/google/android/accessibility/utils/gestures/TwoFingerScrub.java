package com.google.android.accessibility.utils.gestures;

import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.google.android.accessibility.utils.Performance.EventId;

/**
 * Detects a two-finger scrub gesture (left-right-left or right-left-right).
 */
class TwoFingerScrub extends GestureMatcher {
  private static final int REQUIRED_DIRECTION_CHANGES = 2;

  private final float minDelta;
  private boolean tracking = false;
  private float lastAverageX = Float.NaN;
  private int direction = 0;
  private int directionChanges = 0;
  private float totalDistance = 0f;

  TwoFingerScrub(Context context, int gestureId, StateChangeListener listener) {
    super(gestureId, new Handler(context.getMainLooper()), listener);
    int touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    minDelta = Math.max(8f, touchSlop * 0.5f);
  }

  @Override
  public void clear() {
    tracking = false;
    lastAverageX = Float.NaN;
    direction = 0;
    directionChanges = 0;
    totalDistance = 0f;
    super.clear();
  }

  @Override
  protected void onDown(EventId eventId, MotionEvent event) {
    if (event.getPointerCount() > 1) {
      cancelGesture(event);
    }
  }

  @Override
  protected void onPointerDown(EventId eventId, MotionEvent event) {
    if (event.getPointerCount() != 2) {
      cancelGesture(event);
      return;
    }
    tracking = true;
    lastAverageX = averageX(event);
    startGesture(event);
  }

  @Override
  protected void onMove(EventId eventId, MotionEvent event) {
    if (!tracking) {
      return;
    }
    if (event.getPointerCount() != 2) {
      cancelGesture(event);
      return;
    }
    float avgX = averageX(event);
    if (Float.isNaN(lastAverageX)) {
      lastAverageX = avgX;
      return;
    }
    float delta = avgX - lastAverageX;
    if (Math.abs(delta) < minDelta) {
      return;
    }
    int newDirection = delta > 0 ? 1 : -1;
    if (direction == 0) {
      direction = newDirection;
    } else if (newDirection != direction) {
      directionChanges++;
      direction = newDirection;
    }
    totalDistance += Math.abs(delta);
    lastAverageX = avgX;
    if (directionChanges >= REQUIRED_DIRECTION_CHANGES && totalDistance > minDelta * 4) {
      completeGesture(eventId, event);
    }
  }

  @Override
  protected void onPointerUp(EventId eventId, MotionEvent event) {
    finishIfMatched(eventId, event);
  }

  @Override
  protected void onUp(EventId eventId, MotionEvent event) {
    finishIfMatched(eventId, event);
  }

  private void finishIfMatched(EventId eventId, MotionEvent event) {
    if (directionChanges >= REQUIRED_DIRECTION_CHANGES && totalDistance > minDelta * 3) {
      completeGesture(eventId, event);
    } else {
      cancelGesture(event);
    }
  }

  private float averageX(MotionEvent event) {
    float sum = 0f;
    for (int i = 0; i < event.getPointerCount(); i++) {
      sum += event.getX(i);
    }
    return sum / event.getPointerCount();
  }
}
