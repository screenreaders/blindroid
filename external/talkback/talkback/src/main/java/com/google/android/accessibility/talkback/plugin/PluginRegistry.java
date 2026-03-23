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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Registry for BlindReader plugin quick actions. */
public final class PluginRegistry {

  private static final String ACTION_PREFIX = "PLUGIN:";

  private PluginRegistry() {}

  public static boolean isPluginActionKey(@Nullable String actionKey) {
    return actionKey != null && actionKey.startsWith(ACTION_PREFIX);
  }

  public static String buildActionKey(ComponentName componentName) {
    return ACTION_PREFIX + componentName.flattenToString();
  }

  @Nullable
  private static ComponentName parseActionKey(String actionKey) {
    if (!isPluginActionKey(actionKey)) {
      return null;
    }
    String value = actionKey.substring(ACTION_PREFIX.length());
    if (TextUtils.isEmpty(value)) {
      return null;
    }
    return ComponentName.unflattenFromString(value);
  }

  public static List<PluginAction> getQuickActions(Context context) {
    PackageManager pm = context.getPackageManager();
    Intent intent = new Intent(PluginContract.ACTION_PLUGIN_QUICK_ACTION);
    List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);
    if (infos == null || infos.isEmpty()) {
      return Collections.emptyList();
    }

    List<PluginAction> actions = new ArrayList<>();
    for (ResolveInfo info : infos) {
      if (info.activityInfo == null) {
        continue;
      }
      ComponentName component =
          new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
      String label = String.valueOf(info.loadLabel(pm));
      actions.add(new PluginAction(buildActionKey(component), label, component));
    }
    actions.sort(Comparator.comparing(a -> a.label));
    return actions;
  }

  public static List<String> getQuickActionKeys(Context context) {
    List<PluginAction> actions = getQuickActions(context);
    List<String> keys = new ArrayList<>();
    for (PluginAction action : actions) {
      keys.add(action.key);
    }
    return keys;
  }

  public static String getActionLabel(Context context, String actionKey) {
    PluginAction action = findAction(context, actionKey);
    if (action != null && !TextUtils.isEmpty(action.label)) {
      return action.label;
    }
    ComponentName component = parseActionKey(actionKey);
    return component != null ? component.flattenToShortString() : actionKey;
  }

  public static boolean launchAction(Context context, String actionKey) {
    PluginAction action = findAction(context, actionKey);
    ComponentName component = action != null ? action.componentName : parseActionKey(actionKey);
    if (component == null) {
      return false;
    }
    Intent intent = new Intent();
    intent.setComponent(component);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if (intent.resolveActivity(context.getPackageManager()) == null) {
      return false;
    }
    context.startActivity(intent);
    return true;
  }

  @Nullable
  private static PluginAction findAction(Context context, String actionKey) {
    if (!isPluginActionKey(actionKey)) {
      return null;
    }
    for (PluginAction action : getQuickActions(context)) {
      if (TextUtils.equals(action.key, actionKey)) {
        return action;
      }
    }
    return null;
  }

  public static final class PluginAction {
    public final String key;
    public final String label;
    public final ComponentName componentName;

    PluginAction(String key, String label, ComponentName componentName) {
      this.key = key;
      this.label = label;
      this.componentName = componentName;
    }
  }
}
