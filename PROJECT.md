# meTranscriber Project

## Overview
A high-performance offline transcription app for Android using Vosk and Room.

## Architecture
- **UI**: Jetpack Compose with Material 3 Dynamic Color and Glassmorphism.
- **Engine**: Vosk STT Engine for offline processing.
- **Data**: Room DB for persistent storage of sessions and segments.
- **DI**: Manual DI/Constructor injection (Staff level preference for simplicity in this context).

## Current Status
- Migrated from manual SQLite to Room (100% completed).
- Integrated Vosk STT Engine (100% completed).
- Refactored Repository to use Flow (100% completed).
- Implemented premium UI with Material 3 dynamic color system and glassmorphism.
- Resolved coroutine timing hangs in unit tests. All tests now pass cleanly.

## Build Information
- **AGP**: 9.0.1
- **Gradle**: 9.1.0
- **Kotlin**: 2.3.20
- **Room**: 2.6.1
- **Vosk**: 0.3.75
