<div align="center">

# 🎬 CinePulse (Liquid Glass Edition)

**Next-Generation Ultra-Lightweight Android Streaming App**  
*Built with Jetpack Compose, Media3 ExoPlayer, and iOS Liquid-Glass Design*

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-7.0%2B%20(API%2024%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/Media3-ExoPlayer-E50914?style=for-the-badge&logo=youtube&logoColor=white)](https://developer.android.com/guide/topics/media/media3)
[![License: MIT / Modding](https://img.shields.io/badge/License-MIT%20%2F%20Modding-blue?style=for-the-badge)](LICENSE)
[![APK Size](https://img.shields.io/badge/APK%20Size-~3.9%20MB-success?style=for-the-badge)](release/cinepulse-v1.0-release.apk)

<br/>

[⬇️ Download Latest APK](release/cinepulse-v1.0-release.apk) • [✨ Features](#-key-features) • [🎨 Liquid-Glass UI](#-liquid-glass-design-system) • [🛠️ Modding Guide](#-client-modding--customization) • [🚀 Build from Source](#-building-from-source)

</div>

---

## 🌟 Overview

**CinePulse** is an open-source, ultra-lightweight Android streaming client crafted to deliver a modern cinematic experience inspired by Apple TV and Netflix aesthetics. Designed with a custom **iOS Liquid-Glass UI framework**, CinePulse combines translucent specular highlights, fluid spring physics, edge-to-edge transparency, and zero GPU overhead to run smoothly on both flagship devices and low-RAM legacy Android hardware (2GB RAM, Android 7.0+).

The application connects seamlessly to the high-speed **CinePulse Cloud Network** (`movieapi.zarhosting.dpdns.org`) out-of-the-box, allowing developers and modders to jump straight into customizing the client UI, themes, player gestures, and layout components without any complex backend setup.

---

## ✨ Key Features

- **💎 Apple iOS Liquid-Glass Design:** Translucent frosted surfaces, multi-stop specular borders, ambient light sheens, and fluid spring physics on tap.
- **🎯 Strict 3-Color Cinema Palette:**
  - **CinePulse Crimson Red (`#EE1E50` / `#FF2A55`):** Sampled directly from the official CinePulse identity mark.
  - **True OLED Pitch Black (`#000000`):** Battery-saving deep contrast across scaffolds, sheets, and backdrops.
  - **Crisp Pure White (`#FFFFFF`):** High-readability typography, specular borders, and sharp vector iconography.
- **⚡ Ultra-Low RAM & Tiny APK Footprint:** Entire release APK is only **~3.9 MB** with optimized ProGuard rules and zero bloated dependencies.
- **🎥 Modern Streaming Engine (AndroidX Media3 ExoPlayer):**
  - Adaptive 1080p video streaming.
  - Picture-in-Picture (PiP) floating window with clean lifecycle state handling.
  - Multi-language subtitle parser (UTF-8 encoding) with auto-selection and 250ms sync intervals.
  - Responsive playback controls, seek gestures, and audio track selectors.
- **📥 Background Download Manager:** Reliable background downloading integrated with Android system `DownloadManager`, automatically routing saved media to `Downloads/CinePulse/`.
- **📑 Virtualized Episode Pagination:** Long anime and TV series with hundreds of episodes are chunked dynamically into 30-episode pages for instant load times and zero memory spikes.
- **💾 Local SQLite Database (Room):** Offline Watch History, resumable playback progress, and personal Watchlist stored locally on-device.

---

## 🎨 Liquid-Glass Design System

Unlike conventional blur implementations that degrade performance on older Mali and Adreno GPUs, CinePulse uses a calibrated multi-layer composite system:

1. **Specular Glass Edge (`LiquidGlassBorderBrush`):** A custom vertical brush gradient (`#55FFFFFF` top to `#08FFFFFF` bottom) providing realistic light refraction along borders.
2. **Inner Ambient Sheen (`LiquidGlassSheenBrush`):** Linear specular overlays reflecting ambient light across card surfaces.
3. **Fluid Spring Mechanics (`GlassSpringSpec`):** All interactive buttons, chips, and poster cards utilize bouncy damping physics (`DampingRatioMediumBouncy` + `StiffnessMedium`) for responsive tactile feedback.
4. **OLED Contrast Layering:** Deep black glass containers ensure vivid content presentation with zero muddy gray tones.

---

## 🛠️ Client Modding & Customization

CinePulse is open-sourced to encourage community modding, custom UI themes, and client-side innovation!

### What You Can Mod:
- **Themes & Colors:** Modify [`app/src/main/java/com/example/ui/theme/Color.kt`](app/src/main/java/com/example/ui/theme/Color.kt) to create AMOLED neon themes, Cyberpunk palettes, or custom glass shades.
- **Player Controls:** Customize [`app/src/main/java/com/example/ui/player/VideoPlayer.kt`](app/src/main/java/com/example/ui/player/VideoPlayer.kt) with custom scrubbers, gesture controls, or audio visualizers.
- **Glass Components:** Add or restyle cards, badges, and pills in [`app/src/main/java/com/example/ui/components/LiquidGlassComponents.kt`](app/src/main/java/com/example/ui/components/LiquidGlassComponents.kt).
- **Navigation & Layout:** Reorganize home tabs, hero carousels, or catalog filters.

### 🌐 Zero-Setup Backend Connectivity
All mods work out of the box! The client connects securely to the centralized **CinePulse Cloud Media Network** (`movieapi.zarhosting.dpdns.org`). You do not need to configure API tokens, maintain custom databases, or deploy server backends—simply build your mod and enjoy.

> [!NOTE]
> *To protect cloud infrastructure integrity and prevent malicious abuse, backend server protocols, reverse engineering, and internal API documentation are reserved and proprietary. Please focus all modifications and contributions on the Android client application.*

---

## 🚀 Building from Source

### Prerequisites
- **JDK:** OpenJDK 17 or 21
- **Android SDK:** Compile SDK 36, Min SDK 24 (Android 7.0+)
- **Gradle:** 9.3.1 (included via Gradle Wrapper)

### Quick Start
```bash
# Clone the repository
git clone https://github.com/ImAhmedZarir/cinepulse.git
cd cinepulse

# Build Debug APK
./gradlew assembleDebug

# Build Optimized Release APK
./gradlew assembleRelease
```

The output APK will be generated at:
```
app/build/outputs/apk/release/app-release.apk
```

---

## 📦 Project Architecture

```
cinepulse/
├── app/
│   ├── src/main/java/com/example/
│   │   ├── MainActivity.kt               # Edge-to-edge PiP & window lifecycle
│   │   ├── MainViewModel.kt              # App state & playback controller
│   │   ├── data/
│   │   │   ├── local/                    # Room DB (History & Watchlist)
│   │   │   ├── model/                    # Kotlin Moshi data models
│   │   │   └── repository/               # Repository layer with offline caching
│   │   ├── ui/
│   │   │   ├── components/               # Liquid Glass UI primitives, Hero, Cards
│   │   │   ├── player/                   # ExoPlayer engine, Subtitles, Gestures
│   │   │   ├── screens/                  # Home, Catalog, Detail, Search, Library
│   │   │   └── theme/                    # Color tokens, brushes, typography
│   │   └── util/                         # Download helper & device utilities
│   ├── build.gradle.kts                  # App dependencies & ProGuard config
│   └── proguard-rules.pro                # Aggressive code & resource shrinking
├── release/
│   ├── cinepulse-v1.0-release.apk        # Pre-built signed release APK
│   └── README.md                         # Release notes & SHA-256 verification
├── gradle/                               # Version catalogs & wrapper
└── LICENSE                               # Client Modding License
```

---

## 🏷️ Keywords & Topics

`open-sourced-streming-app` • `streaming-app` • `movie-streaming` • `video-streaming` • `media-streaming` • `tv-streaming` • `stream-player` • `video-player` • `exoplayer-streaming` • `media3-exoplayer` • `hls-streaming` • `online-streaming` • `streaming-client` • `movie-app` • `anime-streaming` • `cinema-streaming` • `android-streaming-app` • `live-streaming` • `video-streamer`

---

## 📄 License & Terms

This project is licensed under the **MIT / CinePulse Client Modding License** — see the [LICENSE](LICENSE) file for complete details.

You are welcome and encouraged to mod, customize, theme, and fork the client application. Respect the CinePulse cloud media network and enjoy streaming!

<br/>

<div align="center">
  <sub>Crafted with passion by <a href="https://github.com/ImAhmedZarir">Ahmed Ishtiaque Zarir</a></sub>
</div>
