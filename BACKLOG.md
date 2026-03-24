# Blindroid Backlog (Gap-Closure)

This backlog aggregates remaining gaps to reach feature parity with the references used so far.
Use `[ ]` for TODO and `[x]` for done.

## BlindReader Core (Screen Reader)
- [ ] Multi-engine TTS selection inside BlindReader (not just system-wide).
- [ ] Separate TTS engine for notifications (engine-level, not only rate/pitch profile).
- [ ] Unified Content/OCR settings screen (auto-read, filters, reading style).
- [ ] Advanced OCR modes control (text/object/face/money/scene) in one place.
- [ ] Auto-translation for OCR/text (optional, configurable).
- [ ] Voice assistant actions (voice command execution UI + bindings) parity check.
- [ ] Plugin API for third-party extensions (beyond plugin manager UI).
- [ ] Hardware-trigger actions (e.g., shake device) when available.

## AI Read Screen (Orion Feature Parity)
- [x] Read Screen action integrated into BlindReader actions/gestures.
- [x] Screen snapshot capture pipeline (ScreenshotCapture).
- [x] AI summarization of screen content (summary/detailed prompt).
- [x] API token configuration flow (Gemini settings).
- [x] Floating trigger via floating menu action (acts as overlay button).

## UX / Settings Parity
- [x] Dedicated Assistant/AI settings screen (Gemini settings).
- [ ] Per-feature enable/disable toggles for AI/OCR modules.
- [ ] Usage hints for AI features in onboarding.

## Documentation / Compliance
- [ ] Document AI data flow and privacy controls.
- [ ] Provide offline fallbacks when AI is unavailable.

## Notes
- Jieshuo feature inventory sourced from `info/jieshuo_technical_documentation.xlsx`.
- Orion feature inventory sourced from Orion AI Screen Reader README.
