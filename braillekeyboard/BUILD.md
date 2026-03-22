# Braille Keyboard Build

This module builds as an Android library and is merged into the main Blindroid APK.

## Prerequisites
- Android SDK with build tools.
- Android NDK + CMake (required for liblouis JNI build).
- `local.properties` points to your SDK (Android Studio will create this automatically).

## Build Commands
- Library build (AAR only):
  - `./gradlew :braillekeyboard:assembleDebug`
- Full Blindroid APK (includes IME):
  - `./gradlew :app:assembleRelease`

## Outputs
- `braillekeyboard/build/outputs/aar/braillekeyboard-debug.aar`
- `app/build/outputs/apk/release/app-release.apk`
