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

echo "APK: external/talkback/build/outputs/apk/phone/debug/talkback-phone-debug.apk"
