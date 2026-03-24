# BlindReader Plugin API

This document describes how third‑party apps can expose actions and settings to BlindReader.

## Overview
BlindReader supports two plugin surfaces:
- **Quick actions**: actions that appear in the BlindReader quick menu and can be assigned to gestures.
- **Plugin settings**: a settings screen that appears in BlindReader → Plugins.

Both surfaces are implemented as exported activities in your app with specific intent actions.

## 1) Quick Actions
Create an exported activity that handles the action `com.blindroid.action.PLUGIN_QUICK_ACTION`.

### Manifest example
```xml
<activity
    android:name=".MyBlindReaderActionActivity"
    android:exported="true">
  <intent-filter>
    <action android:name="com.blindroid.action.PLUGIN_QUICK_ACTION" />
    <category android:name="android.intent.category.DEFAULT" />
  </intent-filter>

  <!-- Optional stable id for the action (preferred). -->
  <meta-data
      android:name="com.blindroid.plugin.ACTION_ID"
      android:value="my_app.action.my_feature" />
</activity>
```

### Behavior
- The activity label is shown in BlindReader’s action list.
- When the user triggers the action (quick menu or gesture), BlindReader launches this activity.
- The action should be fast and accessible. Use `FLAG_ACTIVITY_NEW_TASK` handling if needed.

## 2) Plugin Settings
Create an exported activity that handles the action `com.blindroid.action.PLUGIN`.

### Manifest example
```xml
<activity
    android:name=".MyBlindReaderSettingsActivity"
    android:exported="true">
  <intent-filter>
    <action android:name="com.blindroid.action.PLUGIN" />
    <category android:name="android.intent.category.DEFAULT" />
  </intent-filter>
</activity>
```

### Behavior
- BlindReader lists all activities that match this action in Settings → Plugins.
- Selecting the entry opens your settings activity.

## Compatibility Notes
- Keep exported activities lightweight; avoid heavy work on the main thread.
- Provide accessible labels and hints for all UI elements.
- Use a stable `ACTION_ID` to keep gesture bindings across app updates.

## Test Checklist
- Your activity appears in BlindReader quick actions list.
- Your action can be assigned to a gesture and launches successfully.
- Your settings activity appears under BlindReader → Plugins.

