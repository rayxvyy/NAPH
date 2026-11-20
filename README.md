# NAPH

NAPH is a lightweight, dark-mode media center and torrent streaming app for **Linux, Windows, Android, and iOS**.

It starts clean with no pre-installed content or bloated defaults—you add the Stremio addons or media servers you actually use, and customize the layout to look how you want.

---

## What It Does

- **Direct P2P Torrent Streaming** — Streams magnet links and torrent `infoHash` sources sequentially in a temporary cache folder that cleans itself up when you stop watching (or lets you keep downloaded files if you toggle it on in settings).
- **Stremio Addon Support** — Paste any Stremio `manifest.json` or `stremio://` link in **Profile → Content & Discovery → Installed Addons** to load catalogs, streams, and subtitles.
- **Built-in `libmpv` Player** — Plays video directly inside the app window with hardware decoding, subtitle track switching, delay sync (`±100ms`), ASS/SSA subtitle support, and auto-skip for intros/outros. Can also hand off playback to external players (MPV, VLC, IINA).
- **Clean Netflix & Apple TV Layout** — Pure poster cards with no messy catalog badges overlaid on artwork, an Apple TV-style compact left sidebar (`Home`, `Search`, `Library`, `Profile`) that expands on hover, and detailed stream cards showing file size (`MB`/`GB`), seeders, resolution, HDR/Dolby Vision, codec, and audio format.
- **Customizable UI & Optional Ratings** — Choose poster card sizes, corner roundness, theme accents, AMOLED pitch-black mode, and toggle rating sources (IMDb, Rotten Tomatoes, Metacritic, Trakt, MDBList) individually.
- **Debrid & Personal Servers (Optional)** — Connect Real-Debrid, AllDebrid, TorBox, Premiumize, Jellyfin, or Emby if you use them. If you don't connect them, they stay hidden out of your way.
- **Profiles, Cloud Sync & Trackers** — Create or delete household profiles, create a NAPH account to sync addons and watch progress across devices, host a Watch Party room, or connect Trakt.tv and SIMKL for watch history scrobbling.

---

## Downloads

Pre-built packages for **Linux** and **Windows** are available on the [**Releases**](https://github.com/rayxvyy/NAPH/releases) tab.

---

## Build & Run from Source

Requires **JDK 17+** (and `libmpv` installed on Linux/Windows for in-window playback).

```bash
# Run the desktop app
./gradlew :composeApp:run

# Package native installer for your OS (.deb / .msi / .exe)
./gradlew :composeApp:packageDistributionForCurrentOS

# Build universal runnable JAR
./gradlew :composeApp:packageUberJarForCurrentOS
```
