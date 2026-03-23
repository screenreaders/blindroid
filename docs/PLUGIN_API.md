# BlindReader Plugin API (Public)

## Overview
BlindReader exposes a minimal public API for third-party plugins. Plugins can:
- Provide a **Settings** screen that appears in the BlindReader plugin manager.
- Provide **Quick Actions** that appear in the BlindReader Quick Menu.

## Actions
Use the following intent actions in your plugin manifest:
- `com.blindroid.action.PLUGIN` for **settings** entry.
- `com.blindroid.action.PLUGIN_QUICK_ACTION` for **quick actions**.

## Manifest Example (Settings)
```xml
<activity
    android:name=".MyPluginSettingsActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="com.blindroid.action.PLUGIN" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

## Manifest Example (Quick Action)
```xml
<activity
    android:name=".MyPluginQuickAction"
    android:exported="true">
    <intent-filter>
        <action android:name="com.blindroid.action.PLUGIN_QUICK_ACTION" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

## Behavior
- BlindReader discovers quick actions via `PackageManager.queryIntentActivities()`.
- The **label** of the quick action is taken from the activity label.
- The **action key** is generated from the component name and prefixed with `PLUGIN:`.
- Quick actions are enabled by default in the Quick Menu. Users can disable them in Quick Menu settings.

## Notes
- Quick actions should be lightweight and return quickly.
- If your action requires a long operation, show its own UI or a foreground service.
