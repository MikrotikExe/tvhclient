# TVH Client — natívny Tvheadend klient (Android + iOS)

Kotlin Multiplatform projekt: zdieľané jadro (Ktor, modely, secure storage),
natívne UI — Jetpack Compose (Android mobil + TV), SwiftUI (iOS).

Stav: Míľnik M1 — setup obrazovka, multi-server, secure storage, test pripojenia.

## Štruktúra

```
shared/        KMP modul: TvhApi (Ktor, Basic+Digest auth), TvhServer model,
               ServerStore (EncryptedSharedPreferences / Keychain)
androidApp/    Compose UI, jeden APK pre mobil aj Android TV (leanback launcher)
iosApp/        SwiftUI, projekt sa generuje cez XcodeGen
```

## Build — Android

Požiadavky: Android Studio (Ladybug+), JDK 17.

```
cd TvhClient
./gradlew :androidApp:assembleDebug
```

Alebo otvor priečinok v Android Studiu a spusti konfiguráciu androidApp.
Pri prvom otvorení nechaj Gradle stiahnuť wrapper a závislosti — wrapper
nie je v zipe priložený, vygeneruj ho:

```
gradle wrapper --gradle-version 8.10
```

(alebo nechaj Android Studio, ponúkne to samo)

Android TV: rovnaký APK, nainštaluj cez adb install na box/emulátor,
appka je v leanback launcheri.

## Build — iOS

Požiadavky: macOS, Xcode 16+, JDK 17, XcodeGen.

```
brew install xcodegen
cd TvhClient/iosApp
xcodegen
open TvhClient.xcodeproj
```

Xcode pri builde sám zavolá gradle task embedAndSignAppleFrameworkForXcode,
ktorý skompiluje shared modul do Shared.framework.

## Test checklist M1

1. Pridanie servera: host, port 9981, meno/heslo → Otestovať pripojenie
   → očakávaný výsledok: "Pripojené: Tvheadend 4.3-2657 (API v…)"
2. Zlé heslo → "Prihlásenie zlyhalo" (test plain aj digest režim na serveri)
3. Zlý host/port → "Pripojenie zlyhalo: …" do ~5 s (connect timeout)
4. Uloženie, kill appky, znovuotvorenie → server v zozname, aktívny označený
5. Druhý server (napr. IPTVHost aj HP-Server), prepínanie aktívneho
6. Úprava a zmazanie servera
7. Android TV: celý flow čisto D-padom (fokus na poliach, switch, tlačidlá)
8. Jazyk telefónu SK/CZ/EN → preložené texty
9. Overenie, že credentials nie sú v plain texte:
   adb shell + pozri /data/data/sk.tvhclient/shared_prefs/tvh_secure_prefs.xml
   (hodnoty musia byť zašifrované bloby)

## Známe poznámky

- Kód nebol kompilovaný (prostredie bez Android SDK / Xcode). Očakávaj
  drobné chyby typu zlý import alebo verzia — pošli chybový výstup,
  opravím.
- usesCleartextTraffic=true a NSAllowsArbitraryLoads=true sú zámerné,
  TVH beží typicky na HTTP. HTTPS cez prepínač funguje tiež.
- Digest auth: Ktor si vyberie Basic/Digest automaticky podľa 401
  challenge zo servera — netreba nič nastavovať.

## Ďalšie míľniky

M2 kanály+picony, M3 prehrávanie (Media3 / VLCKit), M4 EPG, M5 DVR,
M6 rádio+polish. VLCKit sa pridá v M3 cez SPM: https://code.videolan.org/videolan/VLCKit
