<div align="center">

```
██████╗  █████╗ ███████╗███████╗██████╗ ██╗██████╗ ███████╗
██╔══██╗██╔══██╗██╔════╝██╔════╝██╔══██╗██║██╔══██╗██╔════╝
██████╔╝███████║███████╗███████╗██████╔╝██║██║  ██║█████╗
██╔══██╗██╔══██║╚════██║╚════██║██╔══██╗██║██║  ██║██╔══╝
██████╔╝██║  ██║███████║███████║██║  ██║██║██████╔╝███████╗
╚═════╝ ╚═╝  ╚═╝╚══════╝╚══════╝╚═╝  ╚═╝╚═╝╚═════╝ ╚══════╝
```

### *Your car. Your sound. Every time.*

<br/>

[![Android](https://img.shields.io/badge/Android-API%2029%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Proprietary-FF6B6B?style=for-the-badge)](#license)

<br/>

[![Get it on Google Play](https://img.shields.io/badge/GET%20IT%20ON-Google%20Play-000000?style=for-the-badge&logo=googleplay&logoColor=white)](https://play.google.com/store/apps/details?id=com.velcuri.bassride)

<br/>

</div>

---

## 🚗 What is BassRide?

**BassRide** is an Android equalizer app built from the ground up for the car Bluetooth listening experience — not adapted from a generic EQ tool.

Most EQ apps make you manually switch presets every time you get in the car. BassRide remembers. The moment your phone connects to your car's Bluetooth, your saved EQ profile loads **automatically** — before the music even starts.

> *Connect. Drive. It just sounds right.*

---

## ✨ Features

### Free
| Feature | Details |
|---|---|
| 🎚️ 5-Band Equalizer | System-wide EQ via Android AudioEffect API |
| 💾 2 Custom Presets | Save your own sound signatures |
| 🔵 Bluetooth Detection | Manual BT device detection |
| 🎵 Built-in Presets | Flat, Bass Boost, Vocal Clarity |

### Pro — *One-time unlock, no subscriptions*
| Feature | Details |
|---|---|
| 🎛️ 10-Band Parametric EQ | True parametric control at exact frequencies (32Hz → 16kHz) |
| ♾️ Unlimited Presets | Save as many profiles as you have cars |
| 🔄 Auto-Switch | EQ loads automatically when your car Bluetooth connects |
| 🚘 Per-Device Profiles | Different EQ for every car — sedan, SUV, convertible |
| 🛣️ Road Noise Compensation | Scientific profiles that cut through highway wind noise |
| 🤖 AutoEQ Intelligence | MIT-licensed parametric optimization algorithm |
| 📱 Quick-Access Widget | Swap presets without unlocking your phone |
| 🎛️ Multiband Compression | Professional car cabin dynamics control |
| 🔊 Bass Booster + Virtualizer | Extra punch and spatial width |
| 📤 Import / Export Presets | Back up and share your tuned profiles |

---

## 🎧 How It Works

```
┌─────────────────────────────────────────────────────────────────┐
│                    BassRide Three-Layer Engine                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  🧠 Layer 3 — AutoEQ Intelligence (Pro)                         │
│     AutoEqEngine.kt                                             │
│     Input:  Car target curve (Harman, Road Noise, Night Drive…) │
│     Output: Optimized List<ParametricBand> (freq, gain, Q)      │
│                          │                                       │
│                          ▼  (or manual preset)                   │
│  ⚙️  Layer 1 — DynamicsProcessing API (Core)                    │
│     BassRideDspEngine.kt                                        │
│     Sets exact Hz / gain / Q per band                           │
│     Limiter always active → no BT clipping                      │
│     MBC stage enabled for Pro                                    │
│                          ▲                                       │
│                 attached per audio session                       │
│  📡 Layer 2 — Session Detection                                  │
│     AudioSessionManager.kt                                      │
│     Standard: broadcast receiver (most apps)                    │
│     Enhanced: DUMP permission scan (Spotify, YouTube Music)     │
│                          │                                       │
│                          ▼                                       │
│         🔵 Bluetooth → 🚗 Car Speaker                           │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🎵 Built-In Car Target Curves

BassRide ships with scientifically designed target curves for real driving environments:

| Profile | Best For |
|---|---|
| **Harman Car 2018** | Research-backed reference curve for in-car listening |
| **Flat** | Pure reference — no coloration, what the artist intended |
| **Bass Emphasis** | Compensates for car cabin bass rolloff below 80 Hz |
| **Vocal Clarity** | Mid-forward — podcasts and vocals cut through cabin noise |
| **Road Noise Comp** | 2–6 kHz boost to rise above highway wind and engine rumble |
| **Night Drive** | Warm bass, relaxed treble for quiet late-night cruising |

---

## 📊 EQ Band Frequencies

BassRide uses fully parametric bands at exact frequencies — not locked to whatever your phone's hardware supports.

```
Band  0 ──  32 Hz   ████░░░░░░░░░░░░░░░░  Sub-bass
Band  1 ──  64 Hz   ████████░░░░░░░░░░░░  Bass
Band  2 ── 125 Hz   ████████░░░░░░░░░░░░  Upper bass
Band  3 ── 250 Hz   ██████░░░░░░░░░░░░░░  Low-mid
Band  4 ── 500 Hz   █████░░░░░░░░░░░░░░░  Mid
Band  5 ──  1 kHz   ████░░░░░░░░░░░░░░░░  Upper-mid
Band  6 ──  2 kHz   ███░░░░░░░░░░░░░░░░░  Presence
Band  7 ──  4 kHz   ██░░░░░░░░░░░░░░░░░░  High-mid
Band  8 ──  8 kHz   ██░░░░░░░░░░░░░░░░░░  High
Band  9 ── 16 kHz   █░░░░░░░░░░░░░░░░░░░  Air
```

---

## 🏗️ Architecture

BassRide follows **MVVM + Clean Architecture** across a multi-module project.

```
BassRide/
├── app/              → Entry point, Hilt setup, navigation
├── audio-engine/     → DynamicsProcessing, AutoEQ, session management
├── bluetooth/        → BT detection, device pairing, auto-switching
├── ui/               → All Jetpack Compose screens and components
├── data/             → Room DB, repositories, data models
└── billing/          → Google Play Billing, Pro unlock logic
```

### Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin (no Java) |
| UI | Jetpack Compose |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Database | Room |
| Async | Coroutines + StateFlow |
| Audio | `DynamicsProcessing` API (API 28+) |
| Billing | Google Play Billing Library v6+ |
| Min SDK | API 29 (Android 10) |

---

## 🔐 Permissions

```xml
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```

BassRide explains every permission clearly before requesting it. If a permission is denied, the app shows an explanation — it never silently fails.

---

## 📱 Driver-Safe UI

Every design decision in BassRide's UI is made with driving in mind:

- **56dp minimum touch targets** on all interactive elements
- **18sp+ text** throughout — readable at a glance
- **High contrast** for sun-washed screens
- **Persistent foreground notification** with quick preset switcher — no need to open the app

---

## 🗺️ Development Roadmap

- [x] **Phase 1** — Project scaffold, Hilt DI, Room DB, navigation
- [x] **Phase 2** — Bluetooth detection module, device profiles
- [x] **Phase 3** — Audio engine (DynamicsProcessing + session management)
- [x] **Phase 4** — EQ Screen UI with driver-safe band sliders
- [x] **Phase 5** — Auto-switch & per-device EQ profiles
- [x] **Phase 6** — Google Play Billing & Pro unlock
- [ ] **Phase 7** — Onboarding, dark mode, Play Store polish

---

## 🛠️ Building from Source

> **Note:** BassRide requires access to the Google Play Billing Library and a valid Play Console setup for the Pro purchase flow. The audio engine will work without billing configuration.

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17+
- Android SDK with API 29+ installed

### Steps

```bash
# Clone the repository
git clone https://github.com/velcuri/bassride.git
cd bassride

# Open in Android Studio
# File → Open → select the cloned folder

# Build the debug APK
./gradlew assembleDebug

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

### Module-specific builds

```bash
# Build just the audio engine
./gradlew :audio-engine:build

# Run domain layer unit tests
./gradlew :audio-engine:test
./gradlew :bluetooth:test
./gradlew :data:test
```

---

## 🧪 Testing

| Layer | Strategy |
|---|---|
| UseCases | Unit tests with `TestCoroutineDispatcher` |
| ViewModels | Unit tests with faked repositories |
| Room DB | Integration tests with in-memory database |
| Billing | Manual tests via Play Console test accounts |
| Bluetooth | Manual checklist — BT connect/disconnect cycle |

---

## 🤝 Contributing

BassRide is a private project. Architecture decisions are documented in:

- **`CLAUDE.md`** — Code rules, naming conventions, forbidden patterns
- **`UPDATE.md`** — All major technical decisions with full rationale

Before making any changes to the audio pipeline or billing module, read both files in full.

---

## 📄 License

BassRide is proprietary software. All rights reserved.

The AutoEQ algorithm used in `AutoEqEngine.kt` is reimplemented from the [jaakkopasanen/AutoEq](https://github.com/jaakkopasanen/AutoEq) project, which is MIT licensed.

---

## 📬 Contact

- **Play Store:** [BassRide on Google Play](https://play.google.com/store/apps/details?id=com.velcuri.bassride)
- **Developer:** Velcuri
- **Package:** `com.velcuri.bassride`

---

<div align="center">

**Built for drivers. Tuned for cars. Sounds like nothing else.**

<br/>

[![Get it on Google Play](https://img.shields.io/badge/GET%20IT%20ON-Google%20Play-000000?style=for-the-badge&logo=googleplay&logoColor=white)](https://play.google.com/store/apps/details?id=com.velcuri.bassride)

<br/>

*Made with 🎵 by Velcuri*

</div>
