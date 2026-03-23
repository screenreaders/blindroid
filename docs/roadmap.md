# Blindroid + BlindReader Roadmap (from info/ + Jieshuo strings)

Sources read (local only):
- info/ios.txt
- info/jieshuo_technical_documentation.xlsx
- info/Jieshuo Architecture Pdf (1).pdf
- info/Jieshuo Architecture Pdf (2).pdf
- /home/waldnet/projekty/jieshuo/res/values/strings.xml

## BlindReader

### Gestures & navigation
- [x] iOS gesture parity: navigation & exploration (tap, explore by touch, swipe left/right).
- [x] iOS gesture parity: groups (2-finger swipe right/left to enter/exit group).
- [x] iOS gesture parity: screen jumps (4-finger tap top/bottom).
- [x] iOS gesture parity: reading controls (2-finger swipe up/down, 2-finger tap pause/resume).
- [x] iOS gesture parity: info (3-finger tap for extra info).
- [x] iOS gesture parity: scrolling/page (3-finger swipe up/down/left/right).
- [x] iOS gesture parity: element actions (double-tap, triple-tap, double-tap-hold, split-tap).
- [x] iOS gesture parity: special actions (2-finger double-tap, 2-finger double-tap-hold, 2-finger scrub “Z”).
- [x] iOS gesture parity: VoiceOver control (screen curtain, mute, item chooser, quick settings).
- [x] iOS gesture parity: rotor (two-finger rotate + swipe up/down for action).
- [x] iOS gesture parity: pass-through gestures (double-tap-hold).
- [ ] Dynamic gesture schemes (context-based auto switching).
- [ ] Per-app gesture schemes with quick menu and scheme editor.
- [x] Per-app gesture schemes: edit scope (global vs current app) with per-app overrides.
- [x] Per-app gesture set override (Android/iOS) for current app.
- [x] Per-app gesture set list management (saved overrides).
- [ ] Gesture scheme manager (create/edit/share).
- [x] Per-app gesture set quick access (context menu).
- [ ] Multi-part gestures (double fling).
- [x] Multi-part gestures enable/disable switch (angle + back-and-forth).
- [ ] Edge gestures + edge menus (left/right/bottom) with custom items.
- [x] Edge gestures (left/right edge swipes) with configurable actions.
- [ ] Fingerprint reader gestures (Android 8+).
- [ ] Invert swipe mode (traditional vs default).
- [x] Invert swipe mode (traditional vs default).
- [ ] Quick browsing mode.
- [ ] Node browsing mode.
- [ ] List browsing mode.
- [ ] Automatic browsing + backwards auto.
- [ ] Focus browsing mode.
- [ ] Granular browsing/editing/selection (character/word/line/paragraph/custom).
- [ ] Lift-to-activate mode.
- [ ] Single-tap activate mode.
- [ ] Tap-to-move-focus mode.
- [ ] Hold-to-long-press configuration.
- [ ] Screen curtain (dim screen) toggle and startup behavior.
- [x] Screen curtain (dim screen) toggle and startup behavior.
- [ ] Gaming mode (auto pause touch explore per-app).
- [ ] Content blacklist filtering during browsing.
- [ ] Web browsing handler toggle + web content filtering.

### TTS & audio
- [ ] Multi-engine TTS support.
- [ ] Dual-voice engine (two synthesizers at once).
- [ ] Separate TTS profile for notifications.
- [ ] Audio ducking.
- [ ] Proximity sensor stops speech.
- [ ] Per-mode TTS settings (character-by-character, auto reading).
- [ ] Sound schemes (manager + volume + sounds).
- [ ] AI/TTS: per-language default voice mapping for AI mode.

### Vision & OCR
- [ ] OCR: text recognition (auto on touch).
- [ ] OCR: object recognition.
- [ ] OCR: face recognition.
- [ ] OCR engines: offline + custom/Baidu + language settings.
- [ ] OCR error correction list (regex replacements).
- [ ] Custom OCR extensions (Lua).
- [ ] AI screen reading (Gemini-style): capture + summarize screen content.
- [ ] AI reading modes: summary vs verbatim reading prompts.
- [ ] AI prompt templates per language (multi-language).
- [ ] AI model settings (model name, temperature, safety).
- [ ] AI API key storage + validation.
- [ ] AI capture overlay button (accessibility overlay).
- [ ] AI capture flow (MediaProjection) + foreground service.
- [ ] AI capture file handling (resize, temp file cleanup).
- [ ] AI capture haptic feedback (tick/confirm).
- [ ] AI capture auto-confirm permission (optional, guarded).
- [ ] Virtual screen (inaccessible app) with dark background option.
- [ ] Virtual navigation + virtual keyboard.
- [ ] Visual assistant (camera scenes/people/text).
- [ ] VR visual assistant mode.
- [ ] CAPTCHA recognition.

### Reading & content
- [ ] Automatic subtitle reading.
- [ ] Article reading mode (auto page turn, background reading).
- [ ] List copying.
- [ ] Full-text selection.
- [ ] Favorites.

### Notifications
- [x] Read notifications (bar/toast/internal).
- [x] Read source app for notifications.
- [x] Notification whitelist/blacklist.
- [ ] Read notifications on lock screen.
- [ ] Suppress notifications during reading/auto browsing.

### Assistant & automation
- [ ] Voice assistant core.
- [ ] Custom voice commands + super commands.
- [ ] Voice assistant capabilities (open apps, click elements, run plugins/tools).
- [ ] Voice assistant TTS response settings.
- [ ] Headset/media key integration for assistant.

### Plugins & tools
- [ ] Plugin system (extensions) + manager.
- [ ] Auto-start / scheduled extensions.
- [ ] Tools manager.
- [ ] Clipboard history manager.
- [ ] Label manager (custom labels).

### Backup & sync
- [ ] Backup manager (settings).
- [ ] Cloud backup/restore of gesture schemes.

### Accessibility / system
- [x] Volume key shortcuts.
- [x] Read list index and range.
- [ ] Haptic feedback intensity.
- [ ] Logging + log upload.

## Blindroid (app + system integration)

### Wizard / setup flows
- [ ] Wizard pages for OCR, translation, assistant, plugins.
- [ ] TTS multi-engine selection and notification-voice selection.
- [ ] Per-app gesture scheme selection during setup.

### Integration & modules
- [ ] Module manager: enable/disable OCR/translation/assistant/plugins.
- [ ] Gesture set management UI (Android vs iOS + import/export mappings).
- [ ] Gesture scheme manager entry point (global + per-app).
- [ ] Sound scheme manager entry point.
- [ ] Clipboard history UI + permission handling.
- [ ] Label manager UI.
- [ ] Backup manager UI.
- [ ] Notification reading integration (policy + privacy controls).
- [ ] OCR engine settings UI (offline/online/custom, language).
- [ ] Web browsing handler + content filtering UI.
- [ ] AI screen reading settings UI (API key, language, reading mode, model).
- [ ] AI capture permissions flow (overlay + MediaProjection + foreground service).

## Architecture alignment (Jieshuo-style)
- [ ] Core Engine design doc for BlindReader: event routing, module boundaries, data flow.
- [ ] Module dependency map (Core ↔ TTS ↔ Gestures ↔ OCR ↔ Translation ↔ Assistant ↔ Notifications ↔ Plugins).
