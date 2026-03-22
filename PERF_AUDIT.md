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
