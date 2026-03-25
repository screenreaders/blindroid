# Blindroid – Test Report (IME Smoke)

## Latest Build Status
- Date: 2026-03-25
- APK: `blindroid-180.apk`
- Status: IME smoke run (v180); functional smoke run (API34); connected tests run (API34).

## Latest Unit Tests
- Date: 2026-03-25
- Command: `./gradlew test`
- Result: PASS (no unit tests detected in app, launcher, braillekeyboard)

## Latest Smoke (IME)
- Date: 2026-03-25
- Emulators: `phone_api33`, `phone_api34`, `phone_api35`, `phone_api36`
- APK: `blindroid-180.apk`

### Results
- Android 13 (API 33): PASS – install OK; IME listed, enabled, default set.
- Android 14 (API 34): PASS – install OK; IME listed, enabled, default set.
- Android 15 (API 35): WARN – install timeout; IME listed, enabled, default set (existing install likely present).
- Android 16 (API 36): FAIL – boot incomplete within 4 minutes; IME service unavailable.

## Latest Functional Smoke (App)
- Date: 2026-03-25
- Emulator: `phone_api34`
- APK: `blindroid-180.apk`

### Results
- MainActivity launch: FAIL – `am start` succeeded but process not detected after 3 seconds.

### Notes
- API 36 functional smoke aborted due to emulator boot hang (>6 minutes).

## Latest Instrumentation Tests
- Date: 2026-03-25
- Command: `./gradlew connectedDebugAndroidTest`
- Emulator: `phone_api34`
- Result: PASS (no instrumentation tests detected).

## Previous Smoke (Reference)
- Date: 2026-03-22

## Environment
- Emulators: `phone_api33`, `phone_api34`, `phone_api35`, `phone_api36`
- APK: `blindroid-102.apk`

## Steps (per API)
1. Boot emulator.
2. Install APK.
3. Verify IME listing via `ime list -a`.
4. Enable IME via `ime enable`.
5. Set IME via `ime set`.
6. Verify `settings get secure default_input_method`.

## Results
- Android 13 (API 33): PASS – IME listed, enabled, default set.
- Android 14 (API 34): PASS – IME listed, enabled, default set.
- Android 15 (API 35): PASS – IME listed, enabled, default set.
- Android 16 (API 36): PASS – IME listed, enabled, default set.

## Notes
- API 33 and API 34 required the short component name `com.screenreaders.blindroid/.braillekeyboard.BrailleIME` for `ime enable`/`ime set`.
