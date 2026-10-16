# CinePulse v1.0 Release (Liquid Glass Edition)

## 📦 Release Artifact Details
- **File Name:** `cinepulse-v1.0-release.apk`
- **File Size:** 4,113,263 bytes (~3.92 MB)
- **SHA-256 Checksum:** `32F97349BC4FBDC808CD8696238137E39BD2D15416898C0E131DE734DD99CDF2`
- **Minimum OS Version:** Android 7.0 (API 24)
- **Target OS Version:** Android 16 (API 36)
- **Architecture:** Universal APK (ARM64, ARMv7, x86_64)

---

## 💎 Design & Visual Highlights
- **Strict 3-Color Cinema Palette:**
  - CinePulse Crimson Red (`#EE1E50` / `#FF2A55`)
  - True OLED Pitch Black (`#000000`)
  - Crisp Pure White (`#FFFFFF`)
- **Apple iOS Liquid-Glass UI:** Translucent specular borders (`LiquidGlassBorderBrush`), ambient sheens, and fluid spring physics on all touch elements.
- **Edge-to-Edge Experience:** Seamless integration with system status and navigation bars with cutout safe insets.

---

## ⚡ Performance & Streaming Engine
- **AndroidX Media3 ExoPlayer:** Adaptive 1080p streaming with instant seeking.
- **Picture-in-Picture (PiP):** Floating screen mode with automatic audio shutoff upon window dismissal.
- **Subtitles & Multi-Audio:** UTF-8 SRT subtitle engine with auto-selected default tracks and real-time 250ms sync intervals.
- **Background Downloads:** Integrated with Android `DownloadManager` for reliable downloads to `Downloads/CinePulse/`.
- **Ultra-Low RAM Footprint:** Optimized for smooth 60fps operation even on 2GB RAM devices without GPU blur strain.

---

## 🛠️ Verification
You can verify the integrity of the downloaded APK using PowerShell:
```powershell
Get-FileHash cinepulse-v1.0-release.apk -Algorithm SHA256
```
Expected Hash:
```
32F97349BC4FBDC808CD8696238137E39BD2D15416898C0E131DE734DD99CDF2
```
