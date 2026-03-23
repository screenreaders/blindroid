# BlindReader – Backlog i status funkcji

Ten plik zbiera funkcje do wdrożenia w BlindReaderze oraz ich status. Źródła:
- `info/jieshuo_technical_documentation.xlsx`
- `info/Jieshuo Architecture Pdf (1).pdf`
- `info/Jieshuo Architecture Pdf (2).pdf`
- Repozytoria referencyjne (do przeglądu funkcji): Jieshuo / Orion AI Screen Reader

## Funkcje rdzeniowe (Jieshuo)
- [x] Wielosilnikowe TTS (wspierane przez ustawienia TTS systemu)
- [x] Oddzielny TTS dla powiadomień (rate/pitch + toggle)
- [x] Gesty niestandardowe (mapowanie akcji, gesty wielodotykowe)
- [x] Edge gestures (lewa/prawa krawędź)
- [x] OCR – rozpoznawanie tekstu
- [x] OCR – rozpoznawanie obiektów
- [x] OCR – rozpoznawanie twarzy
- [x] Tłumaczenie tekstu (ACTION_PROCESS_TEXT)
- [x] Asystent/komendy głosowe (akcje podstawowe + szybkie komendy)
- [x] Odczyt powiadomień
- [x] Plugin manager (odkrywanie rozszerzeń)
- [x] Menedżer schowka (historia + menu)

## Funkcje PRO / Zaawansowane
- [x] Auto‑tłumaczenie (auto‑translate dla opisów/rozpoznania)
- [x] Zaawansowane OCR (obiekty, twarze, pieniądze, sceny)
- [x] Makra gestów (sekwencje akcji przypisane do jednego gestu)
- [x] Publiczne API pluginów (SDK/dokumentacja dla rozszerzeń)

## Menu i nawigacja (Jieshuo UI)
- [x] Menu główne ustawień (kategorie)
- [x] Menu kontekstowe akcji
- [x] Menu „Quick” (szybkie akcje)
- [x] Floating menu (pływający przycisk/szybki overlay)

## Ujednolicenie screenreaderów
- [x] Przegląd funkcji Orion AI Screen Reader (README)
- [x] Przegląd funkcji Jieshuo (xls + PDF)
- [x] Scalenie brakujących funkcji w BlindReader

## Audyt Jieshuo (xls)
- [x] Tryb „czytaj wszystko” z dodatkowym filtrowaniem treści (Content Settings)
- [x] OCR auto‑read + filtry + styl czytania (Content Settings)
- [x] Akcja „shake to answer call” (szybkie odebranie połączenia)
- [x] Uporządkowanie ustawień na sekcje: General / TTS / Action / Content / Notification / Advanced

### Orion AI Screen Reader – funkcje do rozważenia
- [x] Pływający przycisk „Read Screen” uruchamiający odczyt zrzutu ekranu (AI)
- [x] Zrzut ekranu + analiza AI z podsumowaniem treści (Gemini)
- [x] Konfiguracja klucza/API tokenu dla usług AI (UI + bezpieczne przechowywanie)
- [x] Tryb „Read Screen” z wibracjami potwierdzającymi działanie

## Prace otwarte
- [x] Makra gestów (projekt + implementacja)
- [x] Floating menu (overlay z szybkim dostępem)
- [x] Scalenie BlindReader z modułem skanowania (akcje OCR kierowane do asystentów skanowania)
- [x] Audyt funkcji Orion/Jieshuo i uzupełnienie braków
