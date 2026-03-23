# Blindroid Roadmap / Checklist

This file tracks upcoming work. Use `[ ]` for TODO and `[x]` for done.

## Launcher Extensions (Post‑v099)
- [x] Review A11y announcements volume/rate with TalkBack (UX polish) — throttle duplicate page announces
- [x] Add configurable page announcement toggle (A11y)
- [x] Add per‑gesture confirmation haptics toggle (UX)

## BlindReader (Screen Reader)
- [x] Audit and replace remaining "TalkBack" branding strings with "BlindReader" (resources)
- [x] Code‑level performance audit (targeted hot‑path optimizations without removing features)
- [x] Reduce allocations in gesture/announcement hot paths (cache resources, reuse buffers)
- [x] Throttle redundant announcements without disabling features (dedupe within short window)
- [x] Add "iOS (VoiceOver) gestures" as selectable gesture set
- [x] Complete iOS gesture parity where Android supports equivalents:
  - [x] 2‑finger “Z” scrub → back/close (mapped to one‑finger back‑and‑forth)
  - [x] Rotor gestures → reading menu actions (kept default up/down behavior)
  - [x] Item Chooser / Quick Settings / Live Recognition equivalents (mapped)
  - [x] 4‑finger top/bottom taps (closest available taps)
- [x] Add onboarding hint to select gesture set (Android vs iOS)
- [x] Verify gesture set labels for non‑PL locales (fallback set to EN)
- [x] Quick menu (gesture‑launched) with customizable action list
- [x] Clipboard history with persistence + quick access menu
- [x] Translate current text action (ACTION_PROCESS_TEXT)
- [x] Notification speech profile (rate/pitch) + toggle
- [x] Object/face recognition actions (mapped to image description/captioning)
- [x] Plugin manager screen (intent‑based discovery)

## Soft Braille Keyboard Reactivation
- [x] Locate Soft Braille Keyboard repo: https://github.com/danieldalton10/Soft-Braille-Keyboard
- [x] Clone repo locally into `external/soft-braille-keyboard`
- [x] Create `braillekeyboard` module inside Blindroid using Soft Braille sources
- [x] Integrate liblouis native translator + tables
- [x] Validate table file mappings against bundled liblouis assets
- [x] Build module and document steps (BUILD.md)
- [x] Update target/compile SDK to latest Android (16)
- [x] Update IME manifest/service configuration for modern Android
- [x] Validate on Android 14 (API 34) with checklist (see VALIDATION.md)
- [x] Validate on Android 15 (API 35) with checklist (see VALIDATION.md)
- [x] Validate on Android 16 (API 36) with checklist (see VALIDATION.md)
- [x] Validate on Android 13 (API 33) with checklist (see VALIDATION.md)
- [x] Integrate/ship inside Blindroid (bundled module + settings entry)

## Android IME Compatibility Checklist
- [x] Review Android 16 behavior changes (all apps + targeting) — edge‑to‑edge opt‑out removed; predictive back requires migration
- [x] Review Android 15 behavior changes (all apps + targeting) — edge‑to‑edge enforced; predictive back animations for opted‑in apps
- [x] Review Android 14 behavior changes (all apps + targeting)
- [x] Verify InputMethodService API usage per Android docs
- [x] Verify window insets / edge‑to‑edge handling in IME UI
- [x] Verify predictive back behavior for IME UI (no custom back handling in IME)
- [x] Verify permissions/exported flags for IME service

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
- [x] Decide delivery: bundled IME module vs standalone APK (bundled)
- [x] Update release pipeline to include IME APK (if standalone) — N/A for bundled
- [x] Add update channel entry for IME (if standalone) — N/A for bundled
