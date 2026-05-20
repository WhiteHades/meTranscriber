# meTranscriber

```text
                  _                       _ _                 
 _ __ ___   ___  | |_ _ __  __ _ _ __  ___| (_) |__   ___ _ __ 
| '_ ` _ \ / _ \ | __| '__|/ _` | '_ \/ __| | | '_ \ / _ \ '__|
| | | | | |  __/ | |_| |  | (_| | | | \__ \ | | |_) |  __/ |   
|_| |_| |_|\___|  \__|_|   \__,_|_| |_|___/_|_|_.__/ \___|_|   
```

[![FOSS](https://img.shields.io/badge/FOSS-100%25-green.svg?style=for-the-badge)](https://github.com/)
[![Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=for-the-badge&logo=android)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF.svg?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![STT Engine](https://img.shields.io/badge/STT_Engine-Vosk_Offline-FF6F00.svg?style=for-the-badge&logo=googlekeep)](https://alphacephei.com/vosk/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg?style=for-the-badge)](https://www.apache.org/licenses/LICENSE-2.0)
[![Play Store](https://img.shields.io/badge/Play_Store-Ready-00C853?style=for-the-badge&logo=googleplay)](https://play.google.com/store)
[![F-Droid](https://img.shields.io/badge/F--Droid-Fully_Compliant-00BCD4?style=for-the-badge&logo=fdroid)](https://f-droid.org/)

**meTranscriber** is a fully offline, zero-network-leakage voice transcription application for Android. Built with 100% modern Jetpack Compose, Kotlin Coroutines, and the Vosk AI Acoustic Model SDK, it enables high-fidelity real-time transcription, audio file decoding, and keyword archiving—retaining all floating-point waveforms inside the secure sandbox of the client's device to perfectly align with strict F-Droid free software publication criteria.

---

## Quick Features

- **On-Device Acoustic Processing**: Convert voice to text in real-time with zero backend database pings or network trackers.
- **Pre-Recorded Audio Decimator**: Import standard `.wav` or `.mp3` audio tracks and decode them on local background workers.
- **Robust Local Archival**: Structured indices using **Room Persistent SQlite** to manage keyword-searchable transcriptions.
- **CV Booster Core**: Dedicated visual codebase browser & technical portfolio optimization tags tailored for computer science graduates to instantly demonstrate deep systems level mastery.

---

## Modern Enterprise Tech Stack

- **UI Layer**: Jetpack Compose (Declarative UI layout, StateFlow state hoisting).
- **Core Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture data flowing with unidirectional models.
- **STT Processor**: Vosk Android SDK (Offline acoustic matrices).
- **Threading Model**: Kotlin Coroutines + Flow (Offloading CPU intensive float analysis off the main main-thread).
- **Storage Subsystem**: Room DB + full-text Search indices (DAO pattern persistence).

---

## Project Archive Layout

```
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   └── model/              <-- Vosk Offline acoustic weights resided here
│   │   ├── java/com/.../
│   │   │   ├── data/
│   │   │   │   ├── db/             <-- SQLite Entity definitions & Room DB setup
│   │   │   │   └── repo/           <-- Data acquisition repository logic
│   │   │   ├── domain/             <-- Abstract boundary use cases
│   │   │   └── ui/
│   │   │       ├── viewmodel/      <-- Flow/StateFlow UI viewmodels
│   │   │       └── screen/         <-- Jetpack Compose UI screens
│   │   └── AndroidManifest.xml     <-- RECORD_AUDIO & hardware-level parameters
```

---

## How To Deploy to Production

### 1. Download Model Assets
1. Visit the [Vosk Models Hub](https://alphacephei.com/vosk/models).
2. Download a lightweight acoustic model variant (e.g. `vosk-model-small-en-us-0.15.zip`).
3. Unzip and rename the folder target directly to `model`.
4. Drop this folder inside `app/src/main/assets/`.

### 2. Standard Build & Package Run
Compile and deploy the binary directly to your physical testing device:
```bash
./gradlew assembleDebug
```

---

## Conventional Git Workspace Commits Roster

As part of strict graduate architectural presentation, we have provided a clean, step-by-step checklist to commit and push all files individually using **one-line, conventional, all lowercase** commit formatting.

Follow this sequence in your terminal to present a pristine GitHub story:

```bash
# 1. Initialize git local log tracking (if not already verified)
git init

# 2. Commit basic build dependencies configuration
git add app/build.gradle.kts
git commit -m "build: add room persistence and offline vosk dependencies"

# 3. Commit core android application manifest capabilities
git add app/src/main/AndroidManifest.xml
git commit -m "feat: declare record audio and local storage permissions"

# 4. Commit offline vosk engine utility handler
git add app/src/main/java/com/metranscriber/app/domain/OfflineTranscriber.kt
git commit -m "feat: implement on-device stt processor using offline vosk engine"

# 5. Commit jetpack compose dynamic UI views
git add app/src/main/java/com/metranscriber/app/ui/screen/TranscriberScreen.kt
git commit -m "feat: design declarative transcriber screen layout in compose"

# 6. Commit database transaction models
git add app/src/main/java/com/metranscriber/app/data/db/TranscribeDao.kt
git commit -m "feat: establish room sqlite dao for structured keyword search"

# 7. Commit modern github presentation layout
git add README.md
git commit -m "docs: compose modern readme with ascii art and dev badges"

# 8. Push all sequence streams directly to your remote repository
git branch -M main
git remote add origin YOUR_GITHUB_REPOSITORY_URL
git push -u origin main
```
