# Aura: Real-Time Audio Fingerprinting, FFT Visualizer & Query by Humming

Aura is a high-performance, native Android application engineered to identify commercial audio tracks and human melodic input (singing, humming) in real time. Operating far beyond simple API wrappers, Aura directly samples acoustic buffers from hardware, performs real-time Fast Fourier Transforms (FFT) on raw 16-bit Pulse-Code Modulation (PCM) streams, extracts spectral fingerprints and melodic pitch contours, and authenticates these acoustic hashes against global databases.

This project bridges theoretical continuous-time and discrete-time signal processing models with modern mobile software architecture, Material 3 design paradigms, and responsive multi-form factor engineering.

![Aura Banner](https://github.com/user-attachments/assets/1fdc32dc-c58b-4fa4-a570-4f2d7c55f176)

---

## Table of Contents
1. [Academic Context](#1-academic-context)
2. [Mathematical Foundation](#2-mathematical-foundation)
   - [Time-Domain Acquisition](#time-domain-acquisition)
   - [Fast Fourier Transform (FFT)](#fast-fourier-transform-fft)
   - [Acoustic Fingerprinting & Spectrogram Hashing](#acoustic-fingerprinting--spectrogram-hashing)
   - [Query by Humming (QbH) & Pitch Contouring](#query-by-humming-qbh--pitch-contouring)
3. [System Architecture & Design](#3-system-architecture--design)
   - [MVVM Layered Architecture](#mvvm-layered-architecture)
   - [Smart Voice Activity Detection (VAD)](#smart-voice-activity-detection-vad)
   - [Intelligent Metadata Localization](#intelligent-metadata-localization)
   - [Demo-Safe Fault Tolerance](#demo-safe-fault-tolerance)
4. [Core Features](#4-core-features)
5. [User Interface & Experience](#5-user-interface--experience)
   - [Samsung-Inspired Quick Settings Floating Orb](#samsung-inspired-quick-settings-floating-orb)
   - [Adaptive Foldable & Split-Screen Engine](#adaptive-foldable--split-screen-engine)
   - [Dynamic Material You & Splash Theming](#dynamic-material-you--splash-theming)
   - [Interactive DSP Explanation Inspector](#interactive-dsp-explanation-inspector)
6. [Technology Stack](#6-technology-stack)
7. [API Integration & Security](#7-api-integration--security)
8. [Repository Structure](#8-repository-structure)
9. [Installation & Setup](#9-installation--setup)
10. [Usage Instructions](#10-usage-instructions)
11. [Future Roadmap](#11-future-roadmap)
12. [License and Citation](#12-license-and-citation)

---

## 1. Academic Context

Initially developed as a practical implementation for the **Signals and Systems** coursework at **Gati Shakti Vishwavidyalaya**, Aura explores how classical Fourier analysis and frequency-domain transformations operate in real-world, stochastic mobile environments.

By capturing hardware audio buffers, computing discrete mathematical transformations at 60 FPS, parsing multi-source acoustic fingerprints, and managing state across diverse device form factors (including foldables and Quick Settings tiles), this project demonstrates end-to-end signal analysis and production-grade Android systems engineering.

---

## 2. Mathematical Foundation

### Time-Domain Acquisition
Audio is acquired directly from the device microphone via Android's low-latency `AudioRecord` API:
* **Sampling Rate ($f_s$):** $44,100\text{ Hz}$ (satisfying the Nyquist-Shannon sampling theorem $f_s > 2B$ for human auditory bandwidth $B \approx 20\text{ kHz}$).
* **Quantization:** 16-bit signed Linear PCM ($65,536$ discrete amplitude levels, dynamic range $\approx 96\text{ dB}$).
* **Channel Configuration:** Mono channel capture windowed into asynchronous power-of-two buffers ($N = 2048$) to eliminate main-thread starvation.

### Fast Fourier Transform (FFT)
A raw PCM array captures amplitude over time $f(t)$, making it sensitive to phase deviations, amplitude variance, and environmental noise. To analyze acoustic harmonics, the signal is transformed into the Frequency Domain $F(\omega)$.

Aura implements an optimized radix-2 decimation-in-time Cooley-Tukey FFT algorithm, reducing computational complexity from $\mathcal{O}(N^2)$ to $\mathcal{O}(N \log N)$:

$$F(\omega) = \int_{-\infty}^{\infty} f(t)e^{-i\omega t}dt$$

For discrete samples $x[n]$ of length $N$:

$$X[k] = \sum_{n=0}^{N-1} x[n] \cdot e^{-i 2\pi k n / N}, \quad k = 0, 1, \dots, N-1$$

The resulting complex vectors yield real and imaginary components used to calculate magnitude spectra:

$$\text{Magnitude}[k] = \sqrt{\text{Re}(X[k])^2 + \text{Im}(X[k])^2}$$

These magnitudes are normalized and rendered in real-time onto the Compose canvas at 60 FPS.

### Acoustic Fingerprinting & Spectrogram Hashing
For master studio recordings:
1. **Spectrogram Generation:** Time-series FFT windows generate a 3D time-frequency-energy distribution.
2. **Local Peak Extraction:** High-energy anchor points (spectral peaks resistant to background noise) are extracted.
3. **Constellation Map Hashing:** Combinatorial pairs of peak frequencies and time differentials $(\Delta t)$ form cryptographic hashes matched against fingerprint indexes.

### Query by Humming (QbH) & Pitch Contouring
For human voice, singing, or humming:
* Standard spectral hashing fails due to vocal timbre variability and polyphonic complexity.
* Aura's QbH pipeline isolates the **fundamental frequency contour ($f_0$ pitch track)** over time, abstracting acoustic audio into invariant melodic intervals matched against global composition and MIDI databases.

---

## 3. System Architecture & Design

Aura is built on modern **Clean Architecture & MVVM (Model-View-ViewModel)** design principles with reactive, unidirectional state flows.

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌────────────────────┐   ┌──────────────────────────────┐  │
│  │ AuraAppScreen (UI) │   │ CaptureActivity (Quick Tile) │  │
│  └─────────┬──────────┘   └──────────────┬───────────────┘  │
│            │                             │                  │
│            ▼                             ▼                  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │         AuraViewModel & OrbStateHolder (State)        │  │
│  └───────────────────────────┬───────────────────────────┘  │
└──────────────────────────────┼──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                       Domain Layer                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  IdentificationRepository  │   LyricsRepository       │  │
│  └───────────────────────────┬───────────────────────────┘  │
└──────────────────────────────┼──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                    Data & Hardware Layer                    │
│  ┌────────────────────────┐  ┌───────────────────────────┐  │
│  │ AudioRecorder & FFT    │  │ AuraDatabase (Room SQL)   │  │
│  └────────────────────────┘  └───────────────────────────┘  │
│  ┌────────────────────────┐  ┌───────────────────────────┐  │
│  │ AcrCloudApi & HMAC-SHA1│  │ AmbientListeningService   │  │
│  └────────────────────────┘  └───────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Smart Voice Activity Detection (VAD)
To prevent generating oversized acoustic buffers during idle monitoring, Aura includes continuous amplitude & spectral energy evaluation. In Quick Settings mode, the recorder stays in low-overhead memory monitoring; once audio energy crosses a defined threshold ($\sum |F(\omega)| > 38$), it triggers active 10-second capture without accumulating leading silence.

### Intelligent Metadata Localization
Melody databases often return non-ASCII regional titles (e.g., Japanese/Korean karaoke tags). Aura includes an algorithmic fallback and sanitation pipeline that prioritizes standard international metadata (ASCII/Latin scripts) for clean UI presentation.

### Demo-Safe Fault Tolerance
To ensure stability during connectivity dropouts or API rate-limiting, the network layer includes intelligent error handling and deterministic mock resolution fallbacks, preventing unhandled crashes or infinite processing states.

---

## 4. Core Features

* **Universal Dual-Mode Identification:** Identifies studio master recordings, live speaker playback, and singing/humming melody lines automatically.
* **Floating Glowing Orb (Quick Settings Tile):** Tap the quick tile from any app or home screen to launch a floating overlay with voice activity detection.
* **Live 60 FPS FFT Visualizer:** Hardware-accelerated spectral frequency bars reacting dynamically to incoming audio.
* **Adaptive Foldable Architecture:** Native multi-window and passport foldable screen support (Galaxy Z Fold, Pixel Fold, tablets), docking results smoothly while preserving the visualizer.
* **Synchronized Lyrics & Artwork Caching:** Fetches track lyrics and high-resolution album artwork with smart fallback search algorithms.
* **Deep Music Streaming Links:** One-tap playback redirection to Spotify and YouTube.
* **Persistent Local History:** Built with Android Room SQLite database to save and replay past discoveries offline.
* **Dynamic Material You & Splash Theming:** Adapts color palettes, UI surfaces, and splash screens directly from the user's Android wallpaper.
* **Interactive DSP Explanation Mode:** In-app educational breakdown displaying mathematical formulas and real-time FFT snapshots.
* **Refined Tactile Immersion:** Discrete haptic feedback for button clicks and successful matches.

---

## 5. User Interface & Experience

### Samsung-Inspired Quick Settings Floating Orb
Tapping Aura's Quick Settings tile invokes `CaptureActivity`, rendering a floating translucent glowing orb directly over the active screen:
* **Idle State:** Displays *"Play, sing or hum a song..."* while running background energy detection.
* **Listening State:** Automatically transitions to *"Listening..."* upon detecting acoustic signals.
* **Cinematic Redirection:** Completes recognition and transitions into the main app with smooth cross-fade animations.

### Adaptive Foldable & Split-Screen Engine
On devices with width $\ge 600\text{dp}$ and height $\ge 600\text{dp}$ (Passport foldables), Aura dynamically splits its interface:
* **Left Screen:** Centers the interactive pulsating audio visualizer and action controls.
* **Right Screen:** Slides in the `ResultBottomSheet` containing metadata, lyrics, and streaming links.
* **Standard Displays:** Automatically falls back to a 72% screen height bottom sheet for single-hand use.

---

## 6. Technology Stack

* **Language:** Kotlin 2.0+
* **UI Framework:** Jetpack Compose (Material Design 3)
* **Architecture:** MVVM, Clean Architecture, Kotlin Coroutines & Flow (`StateFlow`)
* **Local Persistence:** AndroidX Room Database & SQLite
* **Image Loading:** Coil Compose
* **Signal Processing:** Custom Cooley-Tukey Radix-2 FFT Engine & PCM Audio Streamer
* **Cryptography:** `javax.crypto.Mac` (HMAC-SHA1 Dynamic Request Signing)
* **Build System:** Gradle (Kotlin DSL, Version Catalogs)
* **SDK Compatibility:** Min SDK 26 (Android 8.0 Oreo) | Target SDK 35 (Android 15)

---

## 7. API Integration & Security

Aura interfaces with ACRCloud’s `/v1/identify` multi-engine endpoint. 

To maintain cryptographic integrity without embedding static keys, Aura generates dynamic HMAC-SHA1 request signatures per call:

$$\text{Signature} = \text{Base64}\left(\text{HMAC-SHA1}\left(\text{SecretKey}, \text{Method} + \text{"\n"} + \text{URI} + \text{"\n"} + \text{AccessKey} + \text{"\n"} + \text{DataType} + \text{"\n"} + \text{SigVersion} + \text{"\n"} + \text{Timestamp}\right)\right)$$

Sensitive developer credentials (`ACR_HOST`, `ACR_ACCESS_KEY`, `ACR_SECRET_KEY`) are kept in `local.properties` and injected via `BuildConfig` at compile time.

---

## 8. Repository Structure

```text
Aura/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/priyanshu/aura/
│   │       │   ├── MainActivity.kt
│   │       │   ├── audio/
│   │       │   │   ├── AmbientListeningService.kt
│   │       │   │   ├── AudioRecorder.kt
│   │       │   │   ├── AuraQuickTileService.kt
│   │       │   │   ├── CaptureActivity.kt
│   │       │   │   └── FFT.kt
│   │       │   ├── data/
│   │       │   │   ├── AuraDatabase.kt
│   │       │   │   ├── HistoryDao.kt
│   │       │   │   └── HistoryEntity.kt
│   │       │   ├── network/
│   │       │   │   ├── AcrCloudApi.kt
│   │       │   │   ├── ArtworkRepository.kt
│   │       │   │   ├── IdentificationRepository.kt
│   │       │   │   ├── LyricsRepository.kt
│   │       │   │   └── SongResult.kt
│   │       │   ├── ui/
│   │       │   │   ├── AuraAppScreen.kt
│   │       │   │   ├── ExplanationScreen.kt
│   │       │   │   ├── HistoryScreen.kt
│   │       │   │   ├── SettingsScreen.kt
│   │       │   │   └── theme/
│   │       │   │       ├── Color.kt
│   │       │   │       ├── Theme.kt
│   │       │   │       └── Type.kt
│   │       │   └── viewmodel/
│   │       │       ├── AuraState.kt
│   │       │       ├── AuraViewModel.kt
│   │       │       └── OrbStateHolder.kt
│   │       ├── res/
│   │       │   ├── drawable/
│   │       │   ├── values/
│   │       │   └── values-night/
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 9. Installation & Setup

### Prerequisites
* Android Studio (Ladybug / Iguana or later).
* A physical Android device with microphone support (Android 8.0+).
* An [ACRCloud Developer Account](https://console.acrcloud.com/).

### Step-by-Step Setup
1. **Clone the Repository:**
   ```bash
   git clone https://github.com/Ppratik765/Detect-any-music-android-app.git
   cd Detect-any-music-android-app
   ```

2. **Configure Credentials:**
   Create a `local.properties` file in the root directory and add your credentials:
   ```properties
   ACR_HOST="identify-xx-xxxx.acrcloud.com"
   ACR_ACCESS_KEY="your_access_key_here"
   ACR_SECRET_KEY="your_secret_key_here"
   ```

3. **Build and Run:**
   Sync Gradle and deploy the `:app` module to your device.

---

## 10. Usage Instructions

1. **In-App Identification:**
   * Open **Aura** and tap the central pulse button.
   * Play background music or hum/sing a melody.
   * View live frequency spectrums and explore the matched song, lyrics, and links.

2. **Quick Settings Identification:**
   * Pull down the Android notification shade and tap the **Aura** tile.
   * The floating glowing orb will listen, identify the track, and open the result directly.

3. **Signal Inspector:**
   * On any song result, tap **"How this works?"** to inspect the mathematical breakdown and live FFT sample.

---

## 11. Future Roadmap

* [ ] **Bidirectional Playlist Sync:** Auto-add matched tracks into Spotify/YouTube Music playlists.
* [ ] **AI Song Breakdown:** LLM-generated musical analysis (BPM, key signature, mood, production trivia).
* [ ] **Word-by-Word Synced Karaoke:** Real-time synchronized LRC lyrics playback.
* [ ] **Offline Fingerprint Caching:** Offline hash recording with delayed resolution upon network reconnect.
* [ ] **Wear OS Companion App:** Wrist-based audio identification tile.

---

## 12. License and Citation

This project is open-source under the **MIT License**.

If referencing this project for academic or technical research, please cite:

```bibtex
@software{Aura2026,
  author = {Priyanshu Pratik},
  title = {Aura: Real-Time Audio Fingerprinting, FFT Visualizer & Query by Humming},
  year = {2026},
  url = {https://github.com/Ppratik765/Detect-any-music-android-app}
}
```
