# Blindroid Backlog (Gap-Closure)

This backlog aggregates remaining gaps to reach feature parity with the references used so far.
Use `[ ]` for TODO and `[x]` for done.

## BlindReader Core (Screen Reader)
- [x] Multi-engine TTS selection inside BlindReader (not just system-wide).
- [x] Separate TTS engine for notifications (engine-level, not only rate/pitch profile).
- [x] Unified Content/OCR settings screen (auto-read, filters, reading style).
- [x] Advanced OCR modes control (text/object/face/money/scene) in one place.
- [x] Auto-translation for OCR/text (optional, configurable).
- [x] Voice assistant actions (voice command execution UI + bindings) parity check.
- [x] Plugin API for third-party extensions (beyond plugin manager UI).
- [x] Hardware-trigger actions (e.g., shake device) when available.

## AI Read Screen (Orion Feature Parity)
- [x] Read Screen action integrated into BlindReader actions/gestures.
- [x] Screen snapshot capture pipeline (ScreenshotCapture).
- [x] AI summarization of screen content (summary/detailed prompt).
- [x] API token configuration flow (Gemini settings).
- [x] Floating trigger via floating menu action (acts as overlay button).

## UX / Settings Parity
- [x] Dedicated Assistant/AI settings screen (Gemini settings).
- [x] Per-feature enable/disable toggles for AI/OCR modules.
- [x] Usage hints for AI features in onboarding.

## Documentation / Compliance
- [x] Document AI data flow and privacy controls.
- [x] Provide offline fallbacks when AI is unavailable.

## Notes
- Jieshuo feature inventory sourced from `info/jieshuo_technical_documentation.xlsx`.
- Orion feature inventory sourced from Orion AI Screen Reader README.
