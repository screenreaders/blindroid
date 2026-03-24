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

### Potential Optimizations (Not Applied Yet)
- Consider caching Gemini prefs in memory with a preference change listener to avoid repeated `SharedPreferences` lookups on frequent actions. `external/talkback/talkback/src/main/java/com/google/android/accessibility/talkback/actor/gemini/GeminiConfiguration.java`
- Add optional downscale for AI screenshot capture to reduce bitmap memory pressure (especially on lower-end devices). `external/talkback/utils/src/main/java/com/google/android/accessibility/utils/screencapture/ScreenshotCapture.java`
- Evaluate lazy initialization for rarely used actors created in `TalkBackService` (e.g., search, clipboard history, dim screen) to reduce startup cost and idle memory. `external/talkback/talkback/src/main/java/com/google/android/accessibility/talkback/TalkBackService.java`
- Add lightweight telemetry around AI/OCR latency (guarded by developer prefs) to target real bottlenecks before deeper optimizations.
