# meTranscriber Project

## Overview

Offline-first Android transcription app using Vosk for on-device speech recognition and Room for local transcript storage.

## Architecture

- **UI**: Jetpack Compose with Material 3 dynamic color support.
- **State**: `TranscriberViewModel` with `StateFlow` UI state.
- **Audio**: `AudioRecord` streaming through an `AudioRecorder` boundary.
- **Engine**: `TranscriberEngine` abstraction with Vosk implementation and fake test/development implementation.
- **Data**: Room database for sessions and transcript segments.
- **DI**: Manual construction in the ViewModel factory.

## Current Status

- Room persistence is implemented for sessions and segments.
- Live microphone permission and single-stream recording flow are wired.
- WAV import is implemented for 16-bit PCM audio with mono/stereo handling and 16 kHz resampling.
- Vosk engine initialization reports model errors instead of silently falling back.
- JSON export uses Kotlin serialization.
- SRT export uses segment timestamps.
- Android backup is disabled for transcript privacy.
- JVM unit tests and Android test compilation pass locally.

## Known Gaps

- Vosk model assets are not committed.
- MP3 and compressed audio import are not implemented.
- Real-device/emulator instrumentation tests still need to be run outside this session.
- Release signing, Play Store metadata, and F-Droid metadata are not configured.

## Build Information

- **AGP**: 9.0.1
- **Gradle**: 9.1.0
- **Kotlin**: 2.2.0
- **Room**: 2.7.2
- **Vosk**: 0.3.75
- **Compile SDK**: 36
- **Min SDK**: 26
