# Braille Keyboard Validation Checklist

Mark each device/API once verified.

## Devices / API levels
- [ ] Android 13 (API 33)
- [x] Android 14 (API 34)
- [x] Android 15 (API 35)
- [x] Android 16 (API 36)

## Steps (per device)
1. Install APK.
2. Verify IME registered:
   - `adb shell ime list -a`
3. Enable IME:
   - `adb shell ime enable com.screenreaders.blindroid/.braillekeyboard.BrailleIME`
4. Set as default:
   - `adb shell ime set com.screenreaders.blindroid/.braillekeyboard.BrailleIME`
5. Open a text field and enter braille dots.
6. Run diagnostics:
   - Blindroid → Diagnostics → Braille diagnostics.
7. Export report and verify upload (optional).

## Notes
- Tested on emulators on 2026-03-22. For API 34, enabling IME requires the short component name:
  `com.screenreaders.blindroid/.braillekeyboard.BrailleIME`.
