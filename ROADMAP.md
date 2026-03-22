# Blindroid Roadmap / Checklist

This file tracks upcoming work. Use `[ ]` for TODO and `[x]` for done.

## Launcher Extensions (Post‑v099)
- [ ] Review A11y announcements volume/rate with TalkBack (UX polish)
- [ ] Add configurable page announcement toggle (A11y)
- [ ] Add per‑gesture confirmation haptics toggle (UX)

## Soft Braille Keyboard Reactivation
- [ ] Locate Soft Braille Keyboard repo (GitHub URL) and clone locally
- [ ] Build current source and document build steps
- [ ] Update target/compile SDK to latest Android (16)
- [ ] Update IME manifest/service configuration for modern Android
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

## Integrations & Delivery
- [ ] Decide delivery: bundled IME module vs standalone APK
- [ ] Update release pipeline to include IME APK (if standalone)
- [ ] Add update channel entry for IME (if standalone)
