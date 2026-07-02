# meTranscriber

Offline-first Android transcription app built with Kotlin, Jetpack Compose, Room, Coroutines/Flow, and the Vosk Android SDK.

## Current Capabilities

- Live microphone capture at 16 kHz mono PCM.
- WAV file import for 16-bit PCM audio, with mono/stereo handling and 16 kHz resampling.
- Pluggable transcription engine boundary with Vosk and a simulated fallback engine for tests/development.
- Local Room database for transcription sessions and timestamped transcript segments.
- History search across title, transcript text, and notes.
- Editable session notes.
- Share/export as plain text, JSON, and SRT.
- Material 3 Compose UI with dynamic color support.
- Privacy-first manifest: app backup is disabled and backup rule files exclude private data.

## Not Implemented Yet

- `.mp3` import.
- Bundled Vosk model assets.
- Speaker diarization.
- Full-text-search virtual tables.
- Play Store or F-Droid release metadata.

## Project Layout

```text
app/src/main/java/com/metranscriber/app/
├── data/
│   ├── db/                 Room database, DAO, entities
│   └── repository/         Repository boundary and Room implementation
├── domain/model/           Serializable session and segment models
├── engine/                 Transcription engine abstractions and Vosk wrapper
├── engine/audio/           Android AudioRecord streaming
├── theme/                  Material 3 theme values
└── ui/                     Compose screen and ViewModel
```

## Build

Requirements:

- Android SDK with API 36 installed.
- JDK 17.

Useful commands:

```bash
./gradlew --no-daemon -Dkotlin.compiler.execution.strategy=in-process --max-workers=2 :app:testDebugUnitTest
./gradlew --no-daemon -Dkotlin.compiler.execution.strategy=in-process --max-workers=2 :app:lintDebug
./gradlew --no-daemon -Dkotlin.compiler.execution.strategy=in-process --max-workers=2 :app:assembleDebug
```

The low-worker examples reduce CPU pressure on local machines and CI runners.

## Vosk Model Setup

The real Vosk engine expects a model at `app/src/main/assets/model`.

1. Download a compatible model from the Vosk model hub.
2. Unzip it.
3. Rename the extracted folder to `model`.
4. Place it at `app/src/main/assets/model`.

Without model assets, engine initialization reports the missing-model error instead of silently pretending that real transcription is active.

## Verification

CI runs:

- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:assembleDebug`

Instrumentation tests are present under `app/src/androidTest`, but they require an Android device or emulator to execute.

## Privacy

The app does not declare internet access. Transcripts are stored locally in Room. Android backup is disabled in `AndroidManifest.xml`, and backup rule XML files exclude private app data.

## License

Apache License 2.0. See `LICENSE`.
