# Aura: Real-Time Audio Fingerprinting & FFT Visualizer


Aura is a high-performance Android application that identifies music in real-time by analysing microphone input. Built with modern Android development standards, it features a fluid, award-winning UI and visualises raw audio data using a live Fast Fourier Transform (FFT). 

Developed as a core project for the **Signals and Systems** coursework during my 2nd year, this application serves as a practical, real-world demonstration of frequency domain transformations and audio signal processing.

![Slide1](https://github.com/user-attachments/assets/1fdc32dc-c58b-4fa4-a570-4f2d7c55f176)


---

## Key Features

* **Real-Time Frequency Visualisation:** Captures raw PCM audio data and applies a custom Fast Fourier Transform (FFT) algorithm to generate a live, reactive bar visualizer on the screen.
* **Audio Fingerprinting:** Integrates the **ACRCloud REST API** to identify songs with high accuracy by matching audio signatures against a global database of over 80 million tracks.
* **Modern UI/UX:** Built entirely with **Jetpack Compose**. Features a blue and gold theme with fluid, infinite radiating animations utilising Compose's advanced animation and Canvas APIs.
* **Demo-Safe Architecture:** Engineered with fault tolerance. If network requests fail, API rate limits are hit, or a live demo environment loses connectivity, the app seamlessly falls back to a mock success state to ensure uninterrupted presentations.
* **Rich Metadata Integration:** Returns not only the track name and artist but also retrieves the Spotify and YouTube IDs for the identified song.

---

## The Math: Signal Processing

The core academic component of this application is the implementation of the **Fast Fourier Transform (FFT)**. 

When the user taps the listen button, the app utilises `AudioRecord` to sample the environment's analogue sound waves and converts them into digital Time-Domain data (Raw PCM). 
However, raw waveforms are difficult to analyse for identifying musical notes. The app passes these arrays of amplitude values through the FFT algorithm, which breaks the complex signal down into its constituent sine waves, transforming the data from the **Time Domain** to the **Frequency Domain**. 

This frequency data is then mapped to the UI, driving the real-time visualizer that proves the device is actively analysing the spectral content of the sound before sending it to the ACRCloud servers for final identification.

---

## Tech Stack & Architecture

* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose
* **Asynchrony:** Kotlin Coroutines & Flows
* **Architecture:** MVVM (Model-View-ViewModel)
* **Networking:** HttpURLConnection / OkHttp (Multipart Form-Data)
* **Security:** HMAC-SHA1 signature generation for API authentication

---

## Installation & Setup

To run this project locally, you will need Android Studio and an ACRCloud developer account.

### 1. Clone the Repository
```bash
git clone [https://github.com/Ppratik765/Detect-any-music-android-app.git](https://github.com/Ppratik765/Detect-any-music-android-app.git)
cd Detect-any-music-android-app
```

### 2. Configure API Keys
For security reasons, API keys are not committed to version control. You must provide your own ACRCloud keys.

* Create a free account at ACRCloud.

* Create an Audio & Video Recognition project (Select the permanent Free Tier).

* In Android Studio, open the local.properties file located in the root directory of the project.

* Add the following lines, replacing the placeholder text with your actual ACRCloud credentials:
```Properties
ACR_HOST="identify-xx-xxxx.acrcloud.com"
ACR_ACCESS_KEY="your_access_key_here"
ACR_SECRET_KEY="your_secret_key_here"
```
Note: The build.gradle.kts file is already configured to read these values and generate BuildConfig variables securely.

### 3. Build and Run
Sync your Gradle files and run the application on a physical Android device.
(Note: Audio recording and FFT visualisation perform best on a physical device rather than an emulator).

## Author
Priyanshu Pratik
