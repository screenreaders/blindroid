# Performance Audit - TTS + Screenreader

Date: 2026-03-22

## Scope
- BlindReader (TalkBack base) speech pipeline and gesture/action mapping.
- Hot paths that can allocate or speak too frequently.

## Actions Taken
- Added fast duplicate-announcement suppression in `SpeechControllerImpl`.
- Cached action strings in `GestureShortcutMapping` to reduce repeated resource scans.
- Reused temporary holders in `SpeechControllerImpl` to reduce allocations.
- Enabled removal of unnecessary spans in speech output.

## Notes
- This is a static/code-level audit. On-device profiler runs are still recommended for device-specific CPU/battery characteristics.

## 2026-03-24 Audit (Post v142)
### Observations
- Runtime is dominated by TalkBackService’s event pipeline and speech output path.
- AI/OCR work is already gated by feature toggles and network availability, but still incurs screenshot allocation when triggered.

### Applied Optimizations (2026-03-24)
- Cached Gemini prefs with a shared preference listener to reduce repeated reads. `external/talkback/talkback/src/main/java/com/google/android/accessibility/talkback/actor/gemini/GeminiConfiguration.java`
- Added optional downscale for AI screenshot capture to reduce bitmap memory pressure. `external/talkback/utils/src/main/java/com/google/android/accessibility/utils/screencapture/ScreenshotCapture.java`
- Lazy initialization for universal search manager and clipboard history manager when enabled. `external/talkback/talkback/src/main/java/com/google/android/accessibility/talkback/TalkBackService.java`
- Added lightweight AI/OCR latency logging when performance stats are enabled. `external/talkback/talkback/src/main/java/com/google/android/accessibility/talkback/gesture/GestureController.java`, `external/talkback/talkback/src/main/java/com/google/android/accessibility/talkback/actor/ImageCaptioner.java`
- ANR watchdog now starts only when crash reporting is enabled to avoid background CPU wakeups when diagnostics are off. `app/src/main/java/com/screenreaders/blindroid/BlindroidApp.kt`, `app/src/main/java/com/screenreaders/blindroid/diagnostics/AnrWatchdogManager.kt`
