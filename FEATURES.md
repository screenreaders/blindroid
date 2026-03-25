# Blindroid – lista funkcji

## Launcher
- [x] Przyciski nawigacji stron (Poprzedni/Następny)
- [x] Skróty do stron z nazwami i skokiem do wybranej karty
- [x] Zarządzanie skrótami w ustawieniach launchera
- [x] Przełącznik ogłaszania zmian stron (A11y)
- [x] Przełącznik wibracji potwierdzających gesty

## BlindReader (Screen Reader)
- [x] Akcje menu pływającego: Read screen, Scan hub, Item chooser, Live recognition, Voice commands, Clipboard history, Translate text, Describe image, Face recognition, Screen curtain, Quick settings, Read from top, Read from current, Toggle voice feedback, Announce time, Announce battery, Media control, Copy last spoken, Back, Home, Notifications, Toggle speak notifications, Overview, All apps, Screen search, Document scan, Object recognition, Scene recognition, Money recognition, Copy, Paste, Cut, Select all, Repeat last utterance, Spell last utterance, Announce item position, Language options, Macro 1, Macro 2, Macro 3, Braille keyboard, Braille display settings, Toggle braille display, Next/Previous, Containers, Granularity, Windows, Scroll, First/Last in screen, A11y button, Breakouts, Pass-through, Toggles, Gestures tools, Backup, Custom actions, Settings actions, Volume, Click/Long click, Split tap/typing, Editing
- [x] Domyślne skróty quick menu: Item chooser i Live recognition
- [x] Haptyka przy tapnięciu pozycji menu pływającego

## TTS
- [x] Osobny głos i parametry TTS dla powiadomień

## Klawiatura brajlowska
- [x] Moduł IME włączony w Blindroidzie
- [x] Integracja liblouis (JNI + tablice w assets)
- [x] Tłumaczenie brajla z fallbackiem
- [x] Diagnostyka IME (self-test liblouis + tablice)
- [x] Eksport raportu diagnostycznego (udostępnianie)
- [x] Wysyłka raportu diagnostycznego na serwer
- [x] Testy funkcjonalne IME na urządzeniach (smoke: enable/set IME)
- [x] Testy zgodności Android (14/15/16) i stabilność długich sesji (smoke)

## Build i wydania
- [x] Podpisane release APK
- [x] Aktualizacja `update.json`
- [x] Automatyzacja publikacji pakietów (opcjonalnie)

## Backlog (do uzupełnienia)
- [x] Usunięte/wyjaśnione TODO z upstream (PreferenceIME/Speech/MyTranslatorClient)
- [x] Audyt wydajności TTS + screenreader (profiling)
- [x] Dalsze polerowanie UX launchera na podstawie feedbacku
