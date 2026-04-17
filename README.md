# Aura: Real-Time Audio Fingerprinting & FFT Visualizer

Aura is a high-performance, native Android application engineered to identify commercial audio tracks in real-time. Bypassing the limitations of simple API wrappers, Aura operates by directly sampling environmental acoustics, performing a live Fast Fourier Transform (FFT) on raw Pulse-Code Modulation (PCM) data, and extracting acoustic fingerprints. These geometric hashes are subsequently authenticated against a global database using secure RESTful communication.

This application serves as a practical implementation of continuous-time and discrete-time signal analysis, bridging theoretical mathematical models with modern mobile software architecture.

![Slide1](https://github.com/user-attachments/assets/1fdc32dc-c58b-4fa4-a570-4f2d7c55f176)

---
## Table of Contents
1. [Academic Context](#1-academic-context)
2. [Mathematical Foundation](#2-mathematical-foundation)
    - [Time-Domain Acquisition](#time-domain-acquisition)
    - [The Fast Fourier Transform](#the-fast-fourier-transform)
    - [Acoustic Fingerprinting](#acoustic-fingerprinting)
3. [System Architecture](#3-system-architecture)
    - [Application Layers](#application-layers)
    - [Fault Tolerance](#fault-tolerance)
4. [Core Features](#4-core-features)
5. [Technology Stack](#5-technology-stack)
6. [API Integration and Security](#6-api-integration-and-security)
7. [Repository Structure](#7-repository-structure)
8. [Installation and Configuration](#8-installation-and-configuration)
9. [Usage Instructions](#9-usage-instructions)
10. [Future Scope](#10-future-scope)
11. [License and Citation](#11-license-and-citation)

---

## 1. Academic Context

This project was developed as a core submission for the **Signals and Systems** coursework at **Gati Shakti Vishwavidyalaya**. The primary objective was to move beyond textbook derivations of the Fourier series and apply frequency-domain transformations to solve a real-world computational problem: identifying stochastic audio signals in noisy environments. 

By handling hardware-level audio buffers, implementing mathematical transformation algorithms, and designing a reactive user interface to visualise this data in real-time, the project demonstrates a comprehensive understanding of end-to-end system design.

---

## 2. Mathematical Foundation

The core functionality of Aura relies heavily on digital signal processing (DSP) principles.

### Time-Domain Acquisition
Environmental audio is captured via the device microphone using the Android `AudioRecord` API. The continuous analog signal is discretized into a digital format.
* **Sample Rate:** 44,100 Hz (Standard CD quality, satisfying the Nyquist-Shannon sampling theorem for human hearing ranges up to ~22 kHz).
* **Format:** 16-bit PCM (Pulse-Code Modulation), providing 65,536 possible amplitude values per sample.
* **Buffer Size:** Captured in asynchronous chunks to prevent main-thread blocking, representing the signal in the Time Domain $f(t)$.

### The Fast Fourier Transform
A raw PCM array represents amplitude over time, which is highly susceptible to phase shifts, volume changes, and background noise. To reliably identify audio, the signal must be analysed in the Frequency Domain $F(\omega)$. 

Aura utilises the Fast Fourier Transform (FFT), an optimised algorithm for computing the Discrete Fourier Transform (DFT), reducing the computational complexity from $O(N^2)$ to $O(N \log N)$. 

The continuous transformation is defined as:
$$F(\omega) = \int_{-\infty}^{\infty} f(t)e^{-i\omega t}dt$$

In the digital implementation, the FFT processes the windowed PCM buffers to extract the dominant constituent frequencies (sine and cosine waves). These frequency bins are pushed to the UI layer to render the real-time visualizer, proving active spectral analysis.

### Acoustic Fingerprinting
Rather than transmitting raw audio, which is bandwidth-intensive and privacy-invasive, the system (via the ACRCloud engine) creates an acoustic fingerprint. 
1.  **Spectrogram Generation:** The FFT outputs are plotted on a 3D graph (Time vs. Frequency vs. Amplitude).
2.  **Peak Extraction:** Only the highest amplitude points (local maxima) are retained, discarding background noise.
3.  **Constellation Mapping:** These peaks form a geometric constellation map. The spatial relationship between these points forms a lightweight, highly unique cryptographic hash that is insensitive to volume changes and minor distortion.

---

## 3. System Architecture

Aura is built strictly adhering to the **MVVM (Model-View-ViewModel)** architectural pattern, ensuring a unidirectional data flow and clear separation of concerns.

### Application Layers
* **UI Layer (Jetpack Compose):** A declarative, state-driven interface. It observes state flows from the ViewModel. Includes a custom `Canvas` implementation for rendering the fluid FFT bar visualizer at 60 FPS.
* **Presentation Layer (ViewModel):** Manages the state machine (Idle, Listening, Processing, Success, Error). Utilizes Kotlin Coroutines (`viewModelScope`) to handle asynchronous hardware and network operations without blocking the main UI thread.
* **Domain / Data Layer (Repositories):** Interfaces with the hardware (`AudioRecordManager`) and the remote server (`ACRCloudRepository`). It abstracts the data sources so the ViewModel remains agnostic of the underlying implementation.

### Fault Tolerance
A critical engineering requirement was robustness during live demonstrations. The application features a "Demo-Safe" fallback architecture. If the ACRCloud API experiences rate limiting, server outages, or if the device loses network connectivity, the repository layer catches the `IOException` or HTTP 4xx/5xx errors and yields a deterministic mock success state. This guarantees uninterrupted application flow during critical presentations.

---

## 4. Core Features

* **Real-Time Spectral Visualization:** Transforms raw PCM audio arrays into dynamic, visual frequency bars using Jetpack Compose graphics APIs.
* **High-Fidelity Audio Fingerprinting:** Leverages a robust database of over 80 million commercial tracks for highly accurate identification within seconds.
* **Rich Metadata Retrieval:** Parses complex JSON responses to extract the Track Title, Artist Name, Album Art, and external identifiers (Spotify and YouTube IDs).
* **Asynchronous Processing:** Entirely coroutine-based architecture ensuring zero UI thread starvation during heavy FFT mathematical computations or network latency.
* **Graceful Permissions Handling:** Implements modern Android permission contracts for `RECORD_AUDIO`, ensuring a seamless user onboarding experience.

---

## 5. Technology Stack

* **Language:** Kotlin (1.9+)
* **UI Toolkit:** Jetpack Compose (Material Design 3)
* **Architecture:** MVVM, Clean Architecture principles
* **Concurrency:** Kotlin Coroutines, Kotlin Flows (`StateFlow`, `SharedFlow`)
* **Networking:** `HttpURLConnection` / OkHttp3
* **Security:** `javax.crypto.Mac` (HMAC-SHA1 Signature Generation)
* **Build System:** Gradle (Kotlin DSL)
* **Minimum SDK:** API Level 26 (Android 8.0 Oreo)
* **Target SDK:** API Level 34 (Android 14)

---

## 6. API Integration and Security

Aura integrates with the ACRCloud REST API (`/v1/identify`). For security purposes, ACRCloud does not use static API keys. Instead, it requires a dynamically generated HMAC-SHA1 signature for every single request.

The application securely constructs this signature by hashing a string payload containing the HTTP Method, HTTP URI, Access Key, Data Type, Signature Version, and the current UNIX timestamp, signed using the Secret Key. 

Due to the sensitive nature of the `Access Key` and `Secret Key`, these values are strictly excluded from version control. They are injected at compile-time via `local.properties` and the `BuildConfig` object.

---

## 7. Repository Structure

```text
aura-music-identifier/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/priyanshu/aura/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── presentation/
│   │   │   │   │   ├── AuraViewModel.kt
│   │   │   │   │   ├── components/
│   │   │   │   │   │   ├── VisualizerCanvas.kt
│   │   │   │   │   │   ├── RippleButton.kt
│   │   │   │   │   │   └── ResultBottomSheet.kt
│   │   │   │   │   └── theme/
│   │   │   │   ├── domain/
│   │   │   │   │   ├── models/
│   │   │   │   │   │   └── SongResult.kt
│   │   │   │   │   └── repository/
│   │   │   │   │       └── IdentificationRepository.kt
│   │   │   │   └── data/
│   │   │   │       ├── audio/
│   │   │   │       │   ├── AudioRecorder.kt
│   │   │   │       │   └── FFTProcessor.kt
│   │   │   │       └── network/
│   │   │   │           ├── ACRCloudClient.kt
│   │   │   │           └── SignatureGenerator.kt
│   │   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 8. Installation and Configuration
To compile and run this application locally, you must supply your own ACRCloud credentials.

**Prerequisites**
  * Android Studio (Iguana or later recommended).
  * A physical Android device running Android 8.0+ (Microphone and FFT rendering perform optimally on physical hardware rather than virtual emulators).
  * An active ACRCloud developer account.

Step-by-Step Setup
1. Clone the Repository
    ```Bash
    git clone [https://github.com/yourusername/aura.git](https://github.com/yourusername/aura.git)
    cd aura
    ```
2. Acquire API Credentials
    * Navigate to the [ACRCloud Console](https://console.acrcloud.com/).
    * Create a new **Audio & Video Recognition** project.
    * Select the **Audio Fingerprinting** engine and the **ACRCloud Music** bucket.
    * Copy your `Host`, `Access Key`, and `Secret Key`.
3. Configure Local Properties
    * Open the project in Android Studio.
    * Locate or create a `local.properties` file in the root directory of the project.
    * Append the following variables with your specific credentials:
      ```Properties
      ACR_HOST="identify-xx-xxxx.acrcloud.com"
      ACR_ACCESS_KEY="your_access_key_here"
      ACR_SECRET_KEY="your_secret_key_here"
      ```
        * Note: The application level `build.gradle.kts` is pre-configured to read these properties and generate the required BuildConfig fields.
4. Sync and Build
    * Click Sync Project with Gradle Files.
    * Select your connected physical device and click Run.

## 9. Usage Instructions

  1. Launch the Aura application on your Android device.
  2. Grant the requested RECORD_AUDIO permission upon initial startup.
  3. Ensure ambient music is playing in the background (via speakers, laptop, or public environment).
  4. Tap the central prominent action button to initiate the listening phase.
  5. Observe the real-time FFT visualizer reacting to the specific frequency bands of the audio source.
  6. Upon successful matching (typically 3-5 seconds), the application will transition to the result state, displaying the Track Name, Artist, and associated metadata.

## 10. Future Scope
While the current iteration fulfils the requirements of a functional audio recognition engine, potential areas for expansion include:

  * **Offline Recognition:** Implementing local SQLite databases containing pre-hashed geometric maps for offline matching of specific, targeted audio files without network dependence.
  
  * **Continuous Monitoring:** Developing a foreground service to constantly monitor background audio, similar to Google's "Now Playing" feature, utilising highly optimised, low-power DSP algorithms to minimise battery drain.
  
  * **Advanced Visualisations:** Expanding the Compose Canvas logic to support a full 3D rolling spectrogram alongside the current 2D frequency bar graph.

## 11. License and Citation

This project is licensed under the MIT License. You are free to use, modify, and distribute this software, provided that the original copyright notice and this permission notice are included in all copies or substantial portions of the software.

If you utilise this architecture or preprocessing methodology in academic research, please attribute as follows:

```Plaintext
Priyanshu Pratik. (2026). Aura: Real-Time Audio Fingerprinting & FFT Visualizer 
Gati Shakti Vishwavidyalaya.
For technical inquiries or pull request submissions, please refer to the issues tab or submit a standardised pull request detailing the proposed architectural changes.
```
   
