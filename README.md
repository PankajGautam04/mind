<div align="center">

# Mind

**Your private visual & AI memory vault for Android — crafted with frosted Mist-Glass.**

[![Platform](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/PankajGautam04/mind/releases)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Privacy](https://img.shields.io/badge/Storage-100%25%20Local--First-0EA5E9?style=for-the-badge)](#privacy--local-first-by-default)
[![Download](https://img.shields.io/badge/Download-APK%20v1.0.0-6366F1?style=for-the-badge)](https://github.com/PankajGautam04/mind/raw/main/releases/Mind-v1.0.0.apk)

[Download Latest APK](https://github.com/PankajGautam04/mind/raw/main/releases/Mind-v1.0.0.apk) · [Releases](https://github.com/PankajGautam04/mind/releases) · [AI Setup (Gemini / Cloud)](#-optional-cloud-ai-setup-byok--gemini-api) · [Build from Source](#-build-from-source)

</div>

---

## Why Mind?

Every day we take screenshots, copy links, jot down errors, save products, and record quick voice thoughts — and then lose them inside cluttered camera rolls and scattered notes apps.

**Mind** brings everything into one calm, high-speed visual collage on your phone. It works **100% offline out of the box**, and optionally connects to **your own Gemini API key** when you want cloud AI summaries, voice transcription, automatic categorization, and natural-language recall.

---

## Download & Install

> **Direct APK Download:** **[Mind-v1.0.0.apk](https://github.com/PankajGautam04/mind/raw/main/releases/Mind-v1.0.0.apk)**

1. Download **`Mind-v1.0.0.apk`** on your Android phone (`arm64-v8a` · Android 8.0+).
2. Tap the downloaded APK and allow **Install unknown apps** if prompted.
3. Open **Mind** — no account or sign-in required.

---

## Key Features

### Frosted Mist-Glass Visual Experience
- **Zoomable Home Collage**: Pinch in and out (`1×` to `4×` columns) with spring-animated card morphing and instant full-screen blurred photo inspection.
- **24-Color Full-Spectrum OKLCH Theme**: Personalize every glass edge, glow, and card tint across **24 accent colors** in Light, Dark, or Follow System modes.
- **Live Backdrop Blur (`Haze`)**: Real GPU-backed frosted glass across bottom docks, floating buttons, image viewers, and in-app sheets.
- **Custom Capture Wave Animations**: Pick from chromatic glass wave shaders (`Wave Lens`), `Sea Wave`, `Aurora Ribbon`, `Vortex Capsule`, `Starlight Bokeh`, and more.

### Save Anything From Anywhere
- **Voice Memories**: Record local `.m4a` voice notes with animated EQ bars.
- **Quick Notes & Quotes**: Save thoughts, reminders, or code/error snippets in one tap.
- **Links, Photos & Android Sharesheet**: Share images, URLs, audio, or text straight into Mind from any browser or social app.
- **System-Level Triggers**:
  - **Quick Settings Tile** (*Save to Mind*) for 1-tap background screen capture.
  - **Digital Assistant / Long-Press Power Button** integration.
  - **Home Screen Widget** with quick Search, Voice, Note, and `+` Quick Add orbs.
  - **App Shortcuts, Tasker, Automate & MacroDroid** support (`ACTION_CAPTURE_ANYTHING` & Android `CREATE_SHORTCUT` picker).

### Organize & Recall Instantly
- **Smart Spaces & Category Trees**: Auto-grouped topics plus custom Spaces you control.
- **Natural Recall (`Ask Mind`)**: Filter by category, content type, favorites, or search by meaning.
- **Smart Actions**: Automatically extract clickable phone numbers, emails, URLs, and copyable codes/snippets inside Memory Details.

---

## Privacy & Local-First by Default

- **Zero Telemetry, Zero Accounts**: Mind never requires login and sends zero analytics.
- **All Data Stays on Your Device**: Images, `.m4a` recordings, SQLite/Room records, and FTS search indexes are stored strictly inside your device's private app storage.
- **Local-Only Mode Is On by Default**: Until you explicitly add an API key and enable **Gemini Cloud AI**, **zero network requests leave your phone**.

---

## Optional Cloud AI Setup (BYOK — Gemini API)

Mind works completely offline without any API key for saving, organizing, browsing, and keyword searching. If you want **AI summaries, automatic titles/tags, voice transcription, smart categorization, connected memories, and "Ask Mind AI" semantic search**, you can plug in your own **free Google Gemini API key** (stored locally on your device):

### Step 1: Get a Free Gemini API Key
1. Go to **[Google AI Studio](https://aistudio.google.com/apikey)** and sign in with your Google account.
2. Click **Create API key** and copy your key (starts with `AIza...`).

### Step 2: Enable Cloud AI inside Mind
1. Open **Mind** and tap the **Settings** gear icon (top-right of the Home screen).
2. Under the **Intelligence** section:
   - Toggle **Gemini Cloud AI** **ON** (or turn **Local-Only Mode** **OFF**).
   - Tap **API Key & Model** to expand the configuration panel.
   - Paste your key into the **Gemini API Key** field and tap **Save API Key**.
3. **Choose Your Model**:
   - Tap **Fetch Models** to load available Gemini models for your key, select a chip, or type any custom model ID (default: `gemini-3.8-flash`).
4. **Verify**:
   - Tap **Test Connection** — Mind will verify your key and display the response latency in milliseconds.
   - *(Optional)* Tap **Analyze Pending** to enrich memories you previously saved while offline.

### Step 3: Optional Custom Backend Proxy URL
Want to route requests through your own cloud proxy or self-hosted Gemini-compatible gateway?
- Inside **Settings → Intelligence → API Key & Model**, tap **Optional: Custom Backend Proxy URL**, enter your `https://.../` base URL, and tap **Save**.

> You can switch **Local-Only Mode** back ON at any time in Settings to immediately pause all cloud calls while keeping all existing AI summaries cached locally.

---

## Architecture

| Module | Purpose |
| :--- | :--- |
| **`app`** | Jetpack Compose UI (Home Collage, Search, Spaces, Favorites, Memory Detail, Settings), Quick Settings Tile, Assistant Session, App Shortcuts, and Home Screen Widget |
| **`core:designsystem`** | Mist-Glass components, real-time `Haze` blur modifiers, AGSL/Compose distortion wave shaders, and 24-palette OKLCH theme engine |
| **`core:domain`** | Clean Architecture use cases and repository interfaces |
| **`core:data`** | WorkManager async enrichment pipeline, Gemini API client, local media & encrypted key storage |
| **`core:database`** | Room database with FTS4/full-text search, migrations, and default category seeding |
| **`core:model`** | Domain models, capture methods, themes, and preferences |

---

## Build from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/PankajGautam04/mind.git
   ```
2. Open the project in **Android Studio** (JDK 21 / Android Gradle Plugin 8.x).
3. Run `:app:assembleDebug` or click **Run** to deploy to your device.
