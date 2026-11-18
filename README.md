# Mind — Visual & AI Memory Vault for Android

**Mind** is a privacy-first, on-device visual & semantic memory vault for Android. Capture anything — screenshots, photos, voice thoughts, links, quotes, or quick notes — and recall it naturally with AI categorization, OCR, and semantic search.

## Download Latest APK

- **[Download Mind APK (Latest Build)](./releases/Mind-v1.0.0.apk)** *(or grab it from the [Releases](https://github.com/PankajGautam04/mind/releases) tab)*

## Key Highlights

- **Immersive Collage Home Canvas**: Pinch-to-zoom (`1×` to `4×` columns) with elastic spring tile animations, frosted Mist-Glass chrome, and full-screen photo inspection with live background blur.
- **24-Color Full-Spectrum OKLCH Personalization**: Choose from 24 dynamic accent palettes across Light, Dark, and System themes.
- **Instant Multi-Modal Capture**:
  - **Voice Memories**: On-device `.m4a` recording with waveform visualization and AI transcription.
  - **Quick Notes & Quotes**: Instant thought and code-snippet capture.
  - **Links & Photos**: Save URLs or import gallery media, plus full Android Sharesheet integration.
  - **Quick Settings Tile, Long-Press Power Assistant, Widget & Tasker/Automate Shortcuts**: Trigger captures from anywhere on your device.
- **Natural Recall & Smart Spaces**: Search by keyword, category, content type, or ask Mind AI in plain language across your saved memories.
- **100% Local-First Storage**: All memories, media files, and Room/SQLite FTS indices live locally on your device. Bring your own Gemini API key in Settings for optional AI enrichment.

## Architecture

- **`app`**: Jetpack Compose UI (Home Collage, Search, Spaces, Favorites, Memory Detail, Settings), Quick Settings Tile, Digital Assistant session, App Shortcuts, and Home Screen Widget.
- **`core:designsystem`**: Custom Mist-Glass surface components, real-time `Haze` backdrop blur, distortion wave shaders, and 24-color OKLCH theme engine.
- **`core:domain` / `core:data` / `core:database` / `core:model`**: Clean Architecture use cases, Room database + FTS, WorkManager background AI enrichment pipeline, and encrypted local preferences.

## Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/PankajGautam04/mind.git
   ```
2. Open in **Android Studio** and run the `:app:assembleDebug` task (or click **Run**).
