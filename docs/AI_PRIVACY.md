# BlindReader AI Data Flow & Privacy

This document explains how BlindReader handles AI‑assisted features and what data may be sent off‑device.

## AI Features
- **Read Screen (AI)**: Sends a screenshot and prompt to the Gemini API to produce a spoken summary.
- **AI Image Description**: May send a cropped image region for description when explicitly triggered.

## Data Flow
1. The user triggers an AI action (gesture, menu item, or floating button).
2. BlindReader captures a screenshot or image region required for the request.
3. The request is sent to the Gemini API using the user‑provided API key.
4. The response is spoken by BlindReader and not retained.

## Storage
- The Gemini API key is stored locally using encrypted preferences when available.
- AI responses are not stored by BlindReader.

## Controls
- AI features can be disabled in BlindReader settings.
- When AI is disabled or an API key is not set, AI actions fall back to offline behavior.

## Offline Fallbacks
If AI is unavailable (disabled, missing API key, or no network):
- **Read Screen** falls back to standard “Read from top” screen reading.
- **Image text recognition (OCR)** uses on‑device ML Kit and does not require network access.

## User Guidance
- Use AI features only when you are comfortable sending the requested content to the API provider.
- For sensitive content, keep AI disabled or rely on offline OCR/standard reading.

