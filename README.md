# NAPH — Ultra-Snappy Cross-Platform Media Hub & P2P Streaming Engine

**NAPH** is a unified, open-source cross-platform media center built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform** in a single monorepo targeting **Windows, Linux, iOS, and Android**.

---

## Core Feature Suite

### 1. Native Sequential P2P Torrent Streaming Engine (#1 Priority)
* **Sequential Piece Prioritization:** Prioritizes the first 2% (headers / `moov` atom) and last 1% (`MKV Cues`) of video streams so playback starts within seconds.
* **Temporary Ring-Buffer Cache (Default):** Streams torrents in a temporary cache folder and automatically purges downloaded pieces as soon as playback finishes so your PC or phone storage never fills up.
* **Power-User P2P Controls:** Configurable max cache size (2–24 GB), optional **"Keep Downloaded Video Files"** mode, custom save directory, DHT / PEX toggles, speed limits, and optional external **TorrServer** URL support.
* **iOS Sideload Ready:** Full P2P engine included for iOS `.ipa` builds (AltStore / SideStore / Sideloadly).

### 2. Stremio-Compatible JSON Manifest Addons + Debrid + Personal Media Servers
* **Stremio v3 Manifest Protocol:** Install any community `manifest.json` URL or `stremio://` link for catalogs, metadata, P2P torrent streams, Debrid streams, and subtitles.
* **Multi-Debrid Resolver:** Native API support for **Real-Debrid, AllDebrid, TorBox, and Premiumize**.
* **Personal Media Servers:** Direct authentication and playback from self-hosted **Jellyfin** and **Emby** servers right alongside addon catalogs.

### 3. Ultra-Snappy Dark Cinema UI (Apple TV+ / HBO Max / Netflix Inspired)
* **Obsidian Dark Aesthetic (`#070709`):** High-contrast OLED blacks, widescreen Hero Showcase stage, adaptive navigation (Left Cinema Rail on Windows/Linux Desktop, Floating Bottom Bar on iOS/Android Mobile).
* **Aggregated Ratings Bar:** Live scores from **IMDb, Rotten Tomatoes (Critics & Audience), Metacritic, Trakt, and MDBList** on every detail page.
* **In-App YouTube Trailers:** Watch official high-definition trailers directly inside the media detail page without leaving the app.

### 4. Pro Video Player, Auto-Skip & Subtitle Suite
* **Native Playback Engines:** `libmpv` on Windows & Linux, `Media3 ExoPlayer` + `libmpv` on Android, and `MPVKit` (`libmpv`) on iOS.
* **Smart Auto-Skip (IntroDB / AniSkip):** Automatically detects and skips **Intros, Recaps, and Outros/Credits** + Netflix-style **"Up Next Episode"** binge queue.
* **Live Subtitle & `libass` Appearance Editor:** Full ASS/SSA styled anime subtitle rendering, in-player subtitle search, sync delay adjustment (`+/- 100ms`), and live customization of font family, size, colors, background opacity, and vertical position.
* **External Player & Casting:** Handoff streams to **VLC, external MPV, IINA, Infuse, or Outplayer** + **Chromecast & AirPlay** support.

### 5. Free 24/7 Cloud Sync (Supabase), Multi-Profile & Trackers
* **No Local Docker Required:** Uses **Supabase Cloud's Free Tier** (`supabase/migrations/20261120000001_naph_cloud_schema.sql`) to sync data 24/7 across Windows, Linux, iOS, and Android.
* **Netflix-Style Multi-Profile Switcher:** Separate "Continue Watching" progress, watchlists, and preferences per household member.
* **2-Way Tracker Sync:** Real-time scrobbling and history/watchlist sync with **Trakt.tv** and **SIMKL**.

---

## Repository Structure

```text
NAPH/
├── .github/workflows/build-all.yml   # Automated CI builds for Windows, Linux, Android & iOS
├── composeApp/                       # Shared KMP & Compose Multiplatform application
│   └── src/
│       ├── commonMain/               # Shared UI, P2P Engine, Stremio Addons, Debrid, Jellyfin/Emby, Supabase Sync
│       ├── desktopMain/              # Windows & Linux entry point + libmpv bridge
│       ├── androidMain/              # Android Activity + Media3 ExoPlayer surface
│       └── iosMain/                  # iOS ComposeUIViewController + MPVKit / URL scheme bridge
├── iosApp/                           # Xcode SwiftUI wrapper for iOS .ipa builds
└── supabase/migrations/              # Supabase PostgreSQL schema & RLS policies
```

---

## Running & Building

* **Run on Linux / Windows Desktop:**
  ```bash
  ./gradlew :composeApp:run
  ```
* **Package Native Desktop Installers (`.msi`, `.deb`, `.AppImage`):**
  ```bash
  ./gradlew :composeApp:packageDistributionForCurrentOS
  ```
* **Build Android APK:**
  ```bash
  ./gradlew :composeApp:assembleDebug
  ```
