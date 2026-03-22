#!/usr/bin/env bash
set -euo pipefail

SDK_DIR="${ANDROID_SDK:-/home/waldnet/Android/Sdk}"
JDK_DIR="${JAVA_HOME:-/home/waldnet/.local/jdks/jdk-17}"

if [ ! -d "$SDK_DIR" ]; then
  echo "Missing ANDROID_SDK at $SDK_DIR" >&2
  exit 1
fi
if [ ! -d "$JDK_DIR" ]; then
  echo "Missing JAVA_HOME at $JDK_DIR" >&2
  exit 1
fi

GRADLE_VERSION=7.6.4
TMP_DIR="$(mktemp -d)"

echo "sdk.dir=$SDK_DIR" > external/talkback/local.properties

curl -L "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "$TMP_DIR/gradle.zip"
unzip -q -d "$TMP_DIR" "$TMP_DIR/gradle.zip"

JAVA_HOME="$JDK_DIR" PATH="$JDK_DIR/bin:$PATH" \
  "$TMP_DIR/gradle-${GRADLE_VERSION}/bin/gradle" -p external/talkback assembleDebug

APK_PATH="external/talkback/build/outputs/apk/phone/debug/talkback-phone-debug.apk"
ASSET_PATH="app/src/main/assets/blindreader.apk"

if [ ! -f "$APK_PATH" ]; then
  echo "Missing APK at $APK_PATH" >&2
  exit 1
fi

cp -f "$APK_PATH" "$ASSET_PATH"

echo "APK: $APK_PATH"
echo "Asset: $ASSET_PATH"
sha256sum "$ASSET_PATH"
stat -c "size=%s" "$ASSET_PATH"
