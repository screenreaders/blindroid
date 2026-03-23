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

package com.google.android.accessibility.talkback.plugin;

/** Public constants for BlindReader plugin integration. */
public final class PluginContract {

  private PluginContract() {}

  /** Activity action for opening plugin settings. */
  public static final String ACTION_PLUGIN_SETTINGS = "com.blindroid.action.PLUGIN";

  /** Activity action for exposing quick actions in BlindReader. */
  public static final String ACTION_PLUGIN_QUICK_ACTION =
      "com.blindroid.action.PLUGIN_QUICK_ACTION";

  /** Optional metadata key for stable plugin action ids. */
  public static final String META_ACTION_ID = "com.blindroid.plugin.ACTION_ID";
}
