# Blindroid – Test Report (IME Smoke)

Date: 2026-03-22

## Environment
- Emulators: `phone_api34`, `phone_api35`, `phone_api36`
- APK: `app/build/outputs/apk/debug/app-debug.apk`

## Steps (each API)
1. Boot emulator.
2. Install debug APK.
3. Verify IME appears in `ime list -a`.
4. Enable IME: `ime enable com.screenreaders.blindroid/.braillekeyboard.BrailleIME`.
5. Set IME as default: `ime set ...`.
6. Verify `settings get secure default_input_method`.

## Results
- Android 14 (API 34): PASS – IME visible, enable/set OK.
- Android 15 (API 35): PASS – IME visible, enable/set OK.
- Android 16 (API 36): PASS – IME visible, enable/set OK.

## Notes
- This is a smoke test for IME registration and activation.
- Manual typing/gesture tests still recommended on a real device.
