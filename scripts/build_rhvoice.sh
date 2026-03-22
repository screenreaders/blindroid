#!/usr/bin/env bash
set -euo pipefail

SDK_DIR="${ANDROID_SDK:-/home/waldnet/Android/Sdk}"
JDK_DIR="${JAVA_HOME:-/home/waldnet/.local/jdks/jdk-17}"
RHVOICE_ANDROID_DIR="external/rhvoice/src/android"

if [ ! -d "$SDK_DIR" ]; then
  echo "Missing ANDROID_SDK at $SDK_DIR" >&2
  exit 1
fi
if [ ! -d "$JDK_DIR" ]; then
  echo "Missing JAVA_HOME at $JDK_DIR" >&2
  exit 1
fi
if [ ! -x "$RHVOICE_ANDROID_DIR/gradlew" ]; then
  echo "Missing RHVoice gradlew at $RHVOICE_ANDROID_DIR" >&2
  exit 1
fi
if ! command -v scons >/dev/null 2>&1; then
  echo "Missing 'scons' (required for RHVoice build)." >&2
  exit 1
fi

ASSETS_DIR="app/src/main/assets"

mkdir -p "$ASSETS_DIR"
echo "sdk.dir=$SDK_DIR" > "$RHVOICE_ANDROID_DIR/local.properties"

JAVA_HOME="$JDK_DIR" PATH="$JDK_DIR/bin:$PATH" \
  ANDROID_SDK_ROOT="$SDK_DIR" \
  "$RHVOICE_ANDROID_DIR/gradlew" -p "$RHVOICE_ANDROID_DIR" :RHVoice-core:assembleStableDebug

CORE_APK=$(find "$RHVOICE_ANDROID_DIR/RHVoice-core/build/outputs/apk" -type f -name "*.apk" | head -n 1)
if [ -n "$CORE_APK" ]; then
  cp -f "$CORE_APK" "$ASSETS_DIR/rhvoice-core.apk"
  echo "RHVoice core APK copied to $ASSETS_DIR/rhvoice-core.apk"
else
  echo "RHVoice core APK not found." >&2
fi

if [ -n "${RHVOICE_VOICE_TASK:-}" ]; then
  JAVA_HOME="$JDK_DIR" PATH="$JDK_DIR/bin:$PATH" \
    ANDROID_SDK_ROOT="$SDK_DIR" \
    "$RHVOICE_ANDROID_DIR/gradlew" -p "$RHVOICE_ANDROID_DIR" "$RHVOICE_VOICE_TASK"
  VOICE_APK=$(find "$RHVOICE_ANDROID_DIR/RHVoice-data/build/outputs/apk" -type f -name "*.apk" | head -n 1)
  if [ -n "$VOICE_APK" ]; then
    VOICE_ASSET_NAME="${RHVOICE_VOICE_ASSET_NAME:-rhvoice-voice-pl-magda.apk}"
    cp -f "$VOICE_APK" "$ASSETS_DIR/$VOICE_ASSET_NAME"
    echo "RHVoice voice APK copied to $ASSETS_DIR/$VOICE_ASSET_NAME"
  else
    echo "RHVoice voice APK not found." >&2
  fi
fi
