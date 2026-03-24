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

package com.google.android.accessibility.talkback.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import com.google.android.libraries.accessibility.utils.log.LogUtils;

/** Detects shake gestures using the accelerometer. */
public final class ShakeDetector implements SensorEventListener {
  public interface Listener {
    void onShake();
  }

  private static final String TAG = "ShakeDetector";
  private static final long SHAKE_WINDOW_MS = 500;
  private static final long SHAKE_COOLDOWN_MS = 1000;
  private static final int REQUIRED_SHAKES = 2;

  private final SensorManager sensorManager;
  private final Sensor accelerometer;
  private final Listener listener;

  private float thresholdG = 2.7f;
  private boolean running = false;
  private long lastShakeTimeMs = 0;
  private long lastTriggerTimeMs = 0;
  private int shakeCount = 0;

  public ShakeDetector(Context context, Listener listener) {
    this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    this.accelerometer =
        sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    this.listener = listener;
  }

  public boolean isSupported() {
    return sensorManager != null && accelerometer != null;
  }

  public void setThresholdG(float thresholdG) {
    this.thresholdG = thresholdG;
  }

  public void start() {
    if (!isSupported() || running) {
      return;
    }
    sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    running = true;
    reset();
  }

  public void stop() {
    if (!running || sensorManager == null) {
      return;
    }
    sensorManager.unregisterListener(this);
    running = false;
    reset();
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if (!running || event == null || event.values == null || event.values.length < 3) {
      return;
    }
    float x = event.values[0];
    float y = event.values[1];
    float z = event.values[2];
    float gForce =
        (float) Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;

    if (gForce < thresholdG) {
      return;
    }

    long now = SystemClock.uptimeMillis();
    if (lastTriggerTimeMs + SHAKE_COOLDOWN_MS > now) {
      return;
    }

    if (lastShakeTimeMs + SHAKE_WINDOW_MS < now) {
      shakeCount = 0;
    }

    lastShakeTimeMs = now;
    shakeCount++;

    if (shakeCount >= REQUIRED_SHAKES) {
      lastTriggerTimeMs = now;
      shakeCount = 0;
      LogUtils.d(TAG, "Shake detected");
      listener.onShake();
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    // No-op.
  }

  private void reset() {
    lastShakeTimeMs = 0;
    lastTriggerTimeMs = 0;
    shakeCount = 0;
  }
}
