# Blindroid Roadmap / Checklist

This file tracks upcoming work. Use `[ ]` for TODO and `[x]` for done.

## Launcher Extensions (Post‑v099)
- [ ] Review A11y announcements volume/rate with TalkBack (UX polish)
- [ ] Add configurable page announcement toggle (A11y)
- [ ] Add per‑gesture confirmation haptics toggle (UX)

## Soft Braille Keyboard Reactivation
- [x] Locate Soft Braille Keyboard repo: https://github.com/danieldalton10/Soft-Braille-Keyboard
- [x] Clone repo locally into `external/soft-braille-keyboard`
- [x] Create `braillekeyboard` module inside Blindroid using Soft Braille sources
- [ ] Build module and document steps
- [x] Update target/compile SDK to latest Android (16)
- [x] Update IME manifest/service configuration for modern Android
- [ ] Validate on Android 13/14/15/16 (emulators) with checklist
- [ ] Integrate/ship inside Blindroid (module or separate app + installer flow)

## Android IME Compatibility Checklist
- [ ] Review Android 16 behavior changes (all apps + targeting)
- [ ] Review Android 15 behavior changes (all apps + targeting)
- [ ] Review Android 14 behavior changes (all apps + targeting)
- [ ] Verify InputMethodService API usage per Android docs
- [ ] Verify window insets / edge‑to‑edge handling in IME UI
- [ ] Verify predictive back behavior for IME UI
- [ ] Verify permissions/exported flags for IME service

## Reference Links (Android Docs)
- Android 16 behavior changes (all apps): https://developer.android.com/about/versions/16/behavior-changes-all
- Android 16 behavior changes (targeting 16): https://developer.android.com/about/versions/16/behavior-changes-16
- Android 15 behavior changes (all apps): https://developer.android.com/about/versions/15/behavior-changes-all
- Android 15 behavior changes (targeting 15): https://developer.android.com/about/versions/15/behavior-changes-15
- Android 14 behavior changes (all apps): https://developer.android.com/about/versions/14/behavior-changes-all
- Android 14 behavior changes (targeting 14): https://developer.android.com/about/versions/14/behavior-changes-14
- InputMethodService reference: https://developer.android.com/reference/android/inputmethodservice/InputMethodService
- Create an input method (guide): https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method

## Integrations & Delivery
- [ ] Decide delivery: bundled IME module vs standalone APK
- [ ] Update release pipeline to include IME APK (if standalone)
- [ ] Add update channel entry for IME (if standalone)
