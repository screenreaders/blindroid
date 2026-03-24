# Blindroid – Test Report (IME Smoke)

## Latest Build Status
- Date: 2026-03-24
- APK: `blindroid-124.apk`
- Status: NOT RUN (build assembled without emulator run; previous smoke results below)

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
