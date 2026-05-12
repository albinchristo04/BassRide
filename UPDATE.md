# UPDATE.md — BassRide
## Technical Decisions & Change Log

This file tracks all major technical decisions made during planning and development.
Claude Code should read this alongside CLAUDE.md before making architectural changes.

---

## [UPDATE-001] EQ Engine Architecture Decision
**Date:** Pre-development planning
**Status:** ✅ Confirmed

### Decision: Dual-Engine EQ Strategy

BassRide uses two EQ engines layered together — not one.

---

### Engine 1 — Android `AudioEffect` API (Primary)
**Module:** `audio-engine`
**Role:** System-wide EQ applied to all audio output (Spotify, YouTube Music, calls, navigation)

| Property | Detail |
|---|---|
| Cost | Free — built into Android OS |
| Bands | Device-dependent (5–10 bands typical) |
| Audio scope | System-wide via session ID = 0 |
| Latency | Kernel-level, lowest possible |
| Bluetooth | Native support |
| Dependencies | None — Android SDK only |

**Why chosen:**
- Only option that intercepts audio from *other apps* (Spotify, YouTube, etc.)
- No extra library = smaller APK, no licensing risk
- Works natively over Bluetooth — core requirement for BassRide
- Survives app switches and background state via foreground service

**Implementation notes:**
- Always call `Equalizer.getNumberOfBands()` at runtime — never assume 10 bands
- Attach to audio session ID `0` for global effect
- Release via `.release()` on service stop to prevent AudioFlinger leaks
- Pair with `BassBoost` and `Virtualizer` from the same `android.media.audiofx` package
- Free tier: expose 5 bands. Pro tier: expose all available bands up to 10

---

### Engine 2 — Tarsos DSP (Secondary — Pro Feature)
**Module:** `audio-engine/tarsos`
**Role:** High-precision parametric EQ for audio played directly through BassRide

| Property | Detail |
|---|---|
| Cost | Free — open source (Apache 2.0) |
| Bands | Fully customizable — exact frequencies set by developer |
| Audio scope | App-generated audio only (not system-wide) |
| Latency | Medium |
| Bluetooth | Partial — works for audio routed through the app |
| Dependencies | `be.tarsos.dsp:core` |

**Why chosen:**
- Allows fully custom parametric EQ bands at exact Hz values (32, 64, 125... 16kHz)
- Fixes the band frequency limitation of `AudioEffect` (you can't control exact band centers)
- Future path for "Play through BassRide" feature where users pipe audio directly
- Pure Kotlin compatible, no NDK required

**Implementation notes:**
- Tarsos DSP does NOT intercept system audio — only audio your app generates
- Do not use Tarsos as a replacement for `AudioEffect` — they serve different purposes
- Gate behind Pro unlock — only expose when `isProUnlocked == true`
- Add Tarsos dependency only in `audio-engine` module — not app-level

**Gradle dependency:**
```kotlin
// audio-engine/build.gradle.kts
implementation("be.tarsos.dsp:core:2.5")
```

---

### Engines Ruled Out

| Engine | Reason Rejected |
|---|---|
| **Oboe (Google NDK)** | Requires C++/NDK; only affects app-generated audio, not system-wide |
| **SuperpoweredSDK** | Commercial license required for paid apps; NDK complexity; overkill |
| **ViPER4Android** | Requires root — eliminates 95%+ of users; banned from Play Store |

---

### How the Two Engines Work Together

```
User plays Spotify in car via Bluetooth
         ↓
[AudioEffect API — Engine 1]
Applies EQ system-wide at kernel level
         ↓
Car Bluetooth Speaker

─────────────────────────────────────

User plays audio through BassRide directly (Pro feature)
         ↓
[Tarsos DSP — Engine 2]
Applies precision parametric EQ at exact band frequencies
         ↓
[AudioEffect API — Engine 1]
Applies global BT output processing
         ↓
Car Bluetooth Speaker
```

---

### Claude Code Implementation Prompts

**Engine 1 setup:**
```
"In the audio-engine module, create an EqEngine class using android.media.audiofx.Equalizer.
Attach to audio session ID 0 for system-wide effect. Query getNumberOfBands() at runtime and
store the result. Expose applyPreset(preset: PresetEntity) as a suspend function running on
Dispatchers.IO. Add BassBoost and Virtualizer as optional companion effects with device
capability checks. Always call .release() on cleanup. Document band count limitations in
KDoc."
```

**Engine 2 setup (Phase 2 / Pro):**
```
"Add Tarsos DSP (be.tarsos.dsp:core:2.5) to the audio-engine module only. Create a
TarsosEqEngine class that applies a 10-band parametric EQ at exact frequencies:
32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000 Hz. Gate instantiation behind
isProUnlocked check from BillingRepository. This engine processes audio routed directly
through BassRide — do not use it for system-wide audio."
```

---

---

## [UPDATE-002] RootlessJamesDSP Research Findings
**Date:** Pre-development planning
**Status:** ✅ Analysed — Partial adoption decision made

### What RootlessJamesDSP Is

RootlessJamesDSP is an open-source Android app by Tim Schneeberger that implements
a professional-grade system-wide DSP audio engine **without requiring root access**.
Repo: https://github.com/timschneeb/RootlessJamesDSP

---

### The Core Engine: `libjamesdsp`

The heart of the app is `libjamesdsp` — a native C library written by James Fung.
It is far more powerful than Android's built-in `AudioEffect` API.

**Full DSP effects chain inside `libjamesdsp`:**

| Category | Effects |
|---|---|
| **Equalization** | Multimodal Equalizer, Arbitrary Response Equalizer (Graphic EQ) |
| **Dynamics** | Dynamic Range Compander, Dynamic Bass Boost, Output Limiter |
| **Spatial** | Convolver (Impulse Response), Virtual Room Effect, Stereo Wideness, Crossfeed |
| **Enhancement** | ViPER-DDC, Analog Modeling (Tube simulation) |
| **Programmable** | LiveProg DSP (EEL scripting engine for custom effects) |

---

### How It Achieves System-Wide Audio Without Root

This is the key technical insight from the repo:

**Rootless Mode (no root required):**
- Uses Android's **MediaProjection API** to capture audio from other apps
- Processes captured audio through `JamesDspLocalEngine` (JNI wrapper around `libjamesdsp`)
- Re-outputs the processed audio to the system

**Root Mode:**
- Integrates with JamesDSP Magisk module
- Directly accesses system audio streams — no capture limitations

```
[Spotify / YouTube Music / Any App]
         ↓
[MediaProjection API — captures audio stream]
         ↓
[JamesDspLocalEngine — libjamesdsp via JNI]
  → Multimodal EQ
  → Graphic EQ
  → Bass Boost
  → Convolver
  → Virtualizer
         ↓
[Android Audio Output → Bluetooth → Car Speaker]
```

---

### ⚠️ Critical Limitation for BassRide

MediaProjection approach has a **major blocking issue:**

| Limitation | Impact on BassRide |
|---|---|
| **Spotify blocks audio capture** | Our #1 use case app won't work without patching |
| **Google Chrome blocked** | Users playing YouTube in browser won't be processed |
| **SoundCloud blocked** | Same issue |
| **Coexistence issues** | Cannot run alongside other EQ apps using `DynamicsProcessing` API |
| **Added latency** | MediaProjection adds processing delay — noticeable on Bluetooth |

Spotify requires a **ReVanced patch** to remove screen capture protection before
RootlessJamesDSP can process it. This is not acceptable for a mainstream paid app —
we cannot ask users to patch Spotify.

---

### ⚠️ License Issue: GPL-3.0

`libjamesdsp` and RootlessJamesDSP are licensed under **GPL-3.0**.

This means:
- Any app using this code must also be **open-source under GPL-3.0**
- BassRide **cannot use `libjamesdsp` directly** in a commercial closed-source paid app
- Using it would require releasing all of BassRide's source code under GPL

**Decision: Do NOT use `libjamesdsp` or RootlessJamesDSP code in BassRide.**

---

### What We CAN Borrow (Architecture Inspiration Only)

We cannot use their code, but we can study their architecture for ideas:

| Their Approach | BassRide Equivalent |
|---|---|
| `JamesDspLocalEngine` JNI wrapper | Our `EqEngine` wrapping `AudioEffect` API |
| Session polling for audio detection | Our `BluetoothReceiver` for session detection |
| Foreground service keeping DSP alive | Our `BassRideService` foreground service |
| Preset file system | Our Room DB preset system |
| Per-app processing rules | Our per-Bluetooth-device profile system |

---

### Updated Engine Decision

After RootlessJamesDSP analysis, the original dual-engine plan from UPDATE-001 stands:

| Priority | Engine | Reason |
|---|---|---|
| ✅ Primary | Android `AudioEffect` API | No root, no MediaProjection, no Spotify issues, no GPL |
| ✅ Secondary (Pro) | Tarsos DSP | Apache 2.0 license, safe for commercial use |
| ❌ Rejected | `libjamesdsp` / MediaProjection | GPL-3.0 license, Spotify blocked, added latency |

**The `AudioEffect` API remains the right choice for BassRide** — it works natively
over Bluetooth without capture restrictions, requires no special permissions beyond
what we already request, and has zero licensing risk.

---

### Future Consideration

If BassRide ever explores a "Universal Audio Capture" Pro feature in the future,
revisit the MediaProjection approach — but only after:
1. Confirming Spotify's capture policy status at that time
2. Using a custom DSP implementation (not `libjamesdsp`) to avoid GPL
3. Clearly communicating the per-app limitation to users in the UI

---

## [UPDATE-003] MonoTrypt Research — Engine Strategy Revised
**Date:** Pre-development planning
**Status:** ✅ Confirmed — Replaces UPDATE-001 engine plan

### What MonoTrypt Is

MonoTrypt (https://github.com/tryptz/monotrypt.android) is a native Android music
streaming app with a built-in professional-grade AutoEQ engine. Built by tryptz using
the same tech stack as BassRide: Kotlin 2.1, Jetpack Compose, Hilt, Room, ExoPlayer/Media3.

Two key contributions relevant to BassRide:
1. A fully documented **AutoEQ algorithm** (greedy iterative peak-finding with biquad filters)
2. A **Gaussian band mapping model** that delivers parametric EQ precision through the
   native `AudioEffect` API — without needing a second DSP library

---

### ⚠️ License Status

MonoTrypt has no LICENSE file in the repo = **All Rights Reserved by default.**

| Usage type | Allowed |
|---|---|
| Copy code verbatim | ❌ No |
| Use as architecture reference | ✅ Yes |
| Reimplement algorithm logic independently | ✅ Yes |
| Use documented math formulas | ✅ Yes — math is not copyrightable |

**Rule:** Read the algorithm, understand it, rewrite it entirely in our own Kotlin.
Never paste MonoTrypt code directly into BassRide.

---

### FINAL ENGINE ARCHITECTURE — Three-Layer Stack

This replaces the dual-engine plan from UPDATE-001. Tarsos DSP is dropped entirely.

---

#### Layer 1 — Android `AudioEffect` API (Foundation)
**Purpose:** System-wide EQ on all Bluetooth audio output

| Property | Detail |
|---|---|
| Scope | System-wide — affects Spotify, YouTube, any app |
| Latency | Kernel-level, lowest possible |
| Cost | Free — built into Android SDK |
| License | Safe — no third-party code |
| Bands | Device-dependent, queried at runtime |

**Role in BassRide:**
- Core EQ that runs 100% of the time while Bluetooth is connected
- Free tier: expose 5 bands
- Pro tier: expose all available bands (typically 10)
- Pairs with `BassBoost` and `Virtualizer` from same `android.media.audiofx` package

---

#### Layer 2 — Gaussian Band Mapping Model (Precision Layer)
**Purpose:** Deliver true parametric EQ through device's fixed system bands

**Source:** Algorithm concept from MonoTrypt — independently reimplemented in Kotlin

**The problem it solves:**
The `AudioEffect` API gives you fixed bands (e.g. 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz on
Samsung) — you can't choose band center frequencies. This means a 10-band parametric EQ
preset designed at standard frequencies (32, 64, 125Hz...) won't apply correctly.

**How Gaussian mapping solves it:**
For each of the device's fixed system band frequencies, sum the gain contribution from
every parametric band using a Gaussian (bell-curve) model weighted by distance in octaves
and Q factor:

```
For each system band at frequency Fs:
  gain = Σ (parametric_band_gain × e^(-0.5 × (octave_distance / bandwidth)²))
  where octave_distance = |log2(Fs / F_parametric)|
  and   bandwidth = 1 / Q

Clamp result to device millibel limits.
Apply to AudioEffect band.
```

**Result:** A 32 Hz boost in our parametric preset correctly influences the 60Hz system
band, partially influences 230Hz, and has near-zero effect at 910Hz — exactly like a
real parametric filter would behave.

**Implementation class:** `GaussianBandMapper.kt` in `audio-engine` module

```kotlin
// Pseudocode — rewrite fully in Kotlin, do NOT copy from MonoTrypt
class GaussianBandMapper(
    val systemBandFreqs: List<Float>,   // from AudioEffect.getBandFreqRange()
    val systemBandLimits: Pair<Int,Int> // millibel min/max from device
) {
    fun mapParametricToSystem(bands: List<ParametricBand>): List<Int> {
        return systemBandFreqs.map { sysFq ->
            val gain = bands.sumOf { band ->
                val octaveDist = abs(log2(sysFq / band.frequency))
                val bw = 1.0 / band.q
                band.gainDb * exp(-0.5 * (octaveDist / bw).pow(2))
            }
            // Convert dB to millibels and clamp to device limits
            (gain * 100).toInt().coerceIn(systemBandLimits.first, systemBandLimits.second)
        }
    }
}
```

---

#### Layer 3 — AutoEQ Algorithm (Intelligence Layer — Pro)
**Purpose:** Auto-generate correction curves from a target sound profile

**Source:** Algorithm documented in MonoTrypt — independently reimplemented in Kotlin

**What it does:**
Given a target frequency curve (e.g. Harman Car Audio), the algorithm automatically
calculates the optimal 10-band parametric EQ settings that correct the car's audio
output toward that target. Users get a "Fix My Car Audio" button that generates a
custom preset tailored to their target preference.

**The Algorithm (reimplement independently):**

```
AUTOEQ ALGORITHM — BassRide implementation guide:

1. NORMALIZE
   - Take target curve (array of frequency/dB points)
   - Calculate offset between current flat response and target
     over 250–2500 Hz window (perceptual midrange anchor)
   - Compute initial error curve: error[f] = target[f] - current[f]

2. REPEAT up to 10 times (one band per iteration):
   a. FIND WORST DEVIATION
      - Scan 20 Hz to 16 kHz
      - Apply 3-point smoothing: smoothed[i] = (e[i-1] + e[i] + e[i+1]) / 3
      - Weight sub-50 Hz errors by 1.2× (bass needs more correction in cars)
      - Find frequency Fpeak with largest |error|

   b. CALCULATE CORRECTION FILTER
      - gain = -error[Fpeak]           (invert the error)
      - Clamp: ±12 dB (±8 dB above 8 kHz — prevent harsh treble)
      - Scan outward from Fpeak until error drops below 50% of peak error
      - bandwidth_octaves = log2(F_upper / F_lower)
      - Q = sqrt(2^bw) / (2^bw - 1)  clamped to [0.6, 6.0]

   c. COMPUTE BIQUAD RESPONSE and subtract from error curve
      - w0 = 2π × Fpeak / sampleRate
      - A  = 10^(gain / 40)
      - α  = sin(w0) / (2 × Q)
      - b0=1+α×A  b1=-2cos(w0)  b2=1-α×A
      - a0=1+α/A  a1=-2cos(w0)  a2=1-α/A
      - At each freq f: compute |H(f)|² in dB, subtract from error[f]

3. TERMINATE EARLY if max remaining |error| < 0.05 dB

4. SORT resulting bands by frequency (ascending)

5. PASS through GaussianBandMapper → AudioEffect API
```

**Built-in Car Target Curves (Pro feature):**

| Target | Description |
|---|---|
| **Harman Car Audio** | Research-backed preferred car listening curve |
| **Flat** | Completely neutral — reference baseline |
| **Bass Emphasis** | Harman + 3dB shelf below 80Hz for car cabin bass loss |
| **Vocal Clarity** | Mid-forward curve for voice/podcasts in noisy cabins |
| **Road Noise Comp** | Boosts 2–6 kHz to cut through highway wind noise |
| **Night Drive** | Relaxed treble, enhanced bass for quiet night listening |

These are BassRide-original curve definitions — not copied from MonoTrypt.

---

### Full Engine Stack — How All Three Layers Work Together

```
USER ADJUSTS EQ PRESET
(parametric: freq, gain, Q per band)
         ↓
┌─────────────────────────────┐
│  Layer 3 — AutoEQ Algorithm │  (Pro) Auto-generates optimal
│  or manual preset editing   │  parametric bands from target curve
└────────────┬────────────────┘
             ↓
┌─────────────────────────────┐
│  Layer 2 — Gaussian Mapper  │  Converts parametric bands to
│  GaussianBandMapper.kt      │  device's fixed system band gains
└────────────┬────────────────┘
             ↓
┌─────────────────────────────┐
│  Layer 1 — AudioEffect API  │  Applies gains to hardware EQ
│  android.media.audiofx      │  System-wide on ALL audio apps
└────────────┬────────────────┘
             ↓
   Bluetooth → Car Speaker
   🎵 Great sound quality
```

---

### Why This Stack Delivers Great Sound Quality

| Problem | Solution |
|---|---|
| Device EQ bands are fixed & arbitrary | Gaussian mapper distributes parametric gain correctly across whatever bands the device has |
| Manual EQ is hard for most users | AutoEQ generates optimal settings from a known target curve in one tap |
| Car cabin changes frequency response | Road Noise Comp and Harman Car targets correct for real acoustic physics |
| Bass rolloff on BT codecs | Automatic sub-50Hz 1.2× weighting in AutoEQ corrects for this |
| Different cars sound different | Per-device Bluetooth profiles save tailored settings per car |

---

### What Tarsos DSP Was Solving (Now Unnecessary)

UPDATE-001 planned Tarsos DSP as Engine 2 to get precise parametric frequencies.
The Gaussian Band Mapper solves that problem better — it works system-wide (Tarsos
could not) and requires zero extra library dependencies. Tarsos DSP is dropped.

---

### Updated Dependencies

```kotlin
// audio-engine/build.gradle.kts

// REMOVED (was in UPDATE-001):
// implementation("be.tarsos.dsp:core:2.5")  ← dropped, no longer needed

// ADDED: nothing — all three layers use Android SDK only
// Layer 1: android.media.audiofx.* (built-in)
// Layer 2: GaussianBandMapper.kt (our own Kotlin code)
// Layer 3: AutoEqEngine.kt (our own Kotlin code)
```

Zero new external dependencies. Smaller APK. No license risk.

---

### Claude Code Implementation Prompts

**Layer 2 — Gaussian Band Mapper:**
```
"Create GaussianBandMapper.kt in audio-engine/src/main/kotlin/com/velcuri/bassride/audio/.
It takes a list of ParametricBand(frequency: Float, gainDb: Double, q: Double) and maps
them to the device's fixed AudioEffect system bands using a Gaussian distance model.
For each system band frequency, sum contributions from all parametric bands weighted by
exp(-0.5 * (octaveDistance / bandwidth)^2). Convert output dB to millibels and clamp
to device hardware limits. Run on Dispatchers.IO. Include KDoc and unit tests."
```

**Layer 3 — AutoEQ Engine:**
```
"Create AutoEqEngine.kt in audio-engine/src/main/kotlin/com/velcuri/bassride/audio/.
Implement a greedy iterative peak-finding EQ algorithm. Input: a target curve as
List<Pair<Float,Float>> (frequency Hz, gain dB). Output: List<ParametricBand> of up
to 10 bands. Algorithm: normalize over 250–2500 Hz, find worst deviation with 3-point
smoothing and 1.2× sub-50Hz weighting, calculate biquad peaking filter (gain clamped
±12dB, Q clamped 0.6–6.0), update error curve using biquad frequency response, repeat
up to 10 times or until max error < 0.05dB. Gate behind isProUnlocked. Include unit
tests with Harman Over-Ear target as test input."
```

**Built-in Car Targets:**
```
"Create CarTargetCurves.kt in audio-engine with 6 built-in car audio target curves
as constants: HarmanCar, Flat, BassEmphasis, VocalClarity, RoadNoiseComp, NightDrive.
Each is a List<Pair<Float,Float>> of (Hz, dB) breakpoints spanning 20–20000 Hz.
Include factory function getCurveForTarget(CarTarget): List<Pair<Float,Float>>."
```

---

## [UPDATE-004] Web Research — Final Engine Architecture
**Date:** Pre-development planning
**Status:** ✅ CONFIRMED FINAL — Supersedes engine decisions in UPDATE-001 and UPDATE-003

### Research Summary

Three major discoveries that significantly upgrade BassRide's audio engine:
1. `DynamicsProcessing` API replaces plain `AudioEffect.Equalizer` entirely
2. Enhanced Session Detection replaces deprecated session ID 0
3. AutoEQ (MIT license) provides 15,700+ star-validated algorithm we can freely use

---

### 🚨 CRITICAL: Session 0 is Deprecated

> From Android docs: *"Attaching insert effects (equalizer, bass boost, virtualizer)
> to the global audio output mix by use of session 0 is deprecated."*

Our plan in UPDATE-001 and UPDATE-003 used `AudioEffect` attached to session ID 0.
**This approach is deprecated and unreliable on modern Android.**

The correct approach — used by Wavelet and Poweramp EQ — is per-session attachment
using Enhanced Session Detection. See Layer 2 below.

---

### DISCOVERY 1 — DynamicsProcessing API (Replaces AudioEffect.Equalizer)

**What it is:** Android's professional-grade audio effects API introduced in Android 9 (API 28).
Our min SDK is 29 — this works on 100% of our supported devices.

**Why it's better than plain `AudioEffect.Equalizer`:**

| Feature | AudioEffect.Equalizer | DynamicsProcessing |
|---|---|---|
| Band frequencies | Fixed by device OEM | Fully custom — you set exact Hz |
| Band count | Fixed by device (5–10) | You define how many bands |
| Parametric control | No — gain only | Yes — freq + gain + Q per band |
| Multi-band compression | No | Yes — MBC stage |
| Pre-EQ + Post-EQ stages | No | Yes — full DSP chain |
| Limiter | No | Yes — built-in |
| Used by | Basic EQ apps | Wavelet, Poweramp EQ |
| Quality | Basic | Professional-grade |

**DynamicsProcessing full DSP chain:**
```
Input Gain
    ↓
Pre-EQ  (multi-band parametric equalizer — fully configurable freq/Q/gain)
    ↓
MBC     (multi-band compressor — optional, Pro feature)
    ↓
Post-EQ (multi-band parametric equalizer — fully configurable)
    ↓
Limiter (single-band limiter — prevents clipping on BT)
    ↓
Output
```

**Impact on BassRide:**
- TRUE 10-band parametric EQ at exact Hz (32, 64, 125... 16kHz) — no Gaussian mapping needed
- Gaussian Band Mapper from UPDATE-003 is now UNNECESSARY — DynamicsProcessing sets
  exact frequencies directly
- Limiter stage prevents clipping from layered car/phone/app EQ stacking
- MBC stage is a powerful Pro feature (multiband compression for car cabin dynamics)

**Gradle:** No additional dependency — `android.media.audiofx.DynamicsProcessing` is in Android SDK

---

### DISCOVERY 2 — Enhanced Session Detection (Replaces Session 0)

**The problem with session 0:**
Session 0 is deprecated. EQ must attach to specific audio session IDs from media player apps.
But how do you get the session ID from Spotify, YouTube Music, etc.?

**Wavelet's solution — Enhanced Session Detection:**
- Grant app the `android.permission.DUMP` system permission via ADB
- Enable Notification Listener access
- App actively scans system service dumps to detect audio session IDs from any app
- Attach `DynamicsProcessing` to each detected session ID as it opens
- Release when the session closes

**Two session detection modes for BassRide (same as Wavelet):**

| Mode | How it works | Compatible with |
|---|---|---|
| **Standard** | Listens for `ACTION_AUDIO_SESSION_ID` broadcast | Apps that properly broadcast (most music players) |
| **Enhanced** (Pro) | DUMP permission + scan system services | Spotify, YouTube Music, most streaming apps |

**Implementation:**
```
Standard mode:
  BroadcastReceiver listens for android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION
  On receive → attach DynamicsProcessing to session ID in extras

Enhanced mode (requires DUMP permission):
  AudioSessionObserver polls DumpManager every 500ms
  Parse audio sessions from system service dump
  Attach DynamicsProcessing to any new untracked session IDs
  Release when sessions disappear from dump
```

**DUMP permission setup (user flow):**
Guide user to grant via ADB command OR use Shizuku library (no PC needed):
```
pm grant com.velcuri.bassride android.permission.DUMP
```
Shizuku allows granting this permission from phone only — no computer needed.

---

### DISCOVERY 3 — AutoEQ Project (MIT License — Freely Usable)

**What it is:** jaakkopasanen/AutoEq on GitHub — 15,700+ stars, MIT licensed.
The gold standard AutoEQ algorithm used by Wavelet, Poweramp EQ, and thousands of apps.

**License:** MIT — fully free for commercial use, no source disclosure required.

**What we get from it:**
- The parametric EQ optimization algorithm (MIT — can use in BassRide)
- Pre-computed results for 700+ headphone models (useful reference data)
- Harman target curve data files (frequency/dB CSV — freely usable)
- Algorithm validated by 15,700+ stars and used in production by Wavelet

**AutoEQ Algorithm (MIT — reimplement in Kotlin):**
The algorithm inverts the error function: error = measurement - target curve.
Then iteratively fits parametric biquad filters to flatten the error.
Identical in principle to the MonoTrypt algorithm in UPDATE-003 — now confirmed
to be MIT licensed via the original jaakkopasanen source.

---

### FINAL ENGINE ARCHITECTURE — Three Layers (Revised)

The Gaussian Band Mapper from UPDATE-003 is dropped — not needed with DynamicsProcessing.

---

#### Layer 1 — DynamicsProcessing API (Core EQ Engine)

```kotlin
// BassRideDspEngine.kt
val config = DynamicsProcessing.Config.Builder(
    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
    inputGain = 0f,
    applyBandLimits = false,
    preEqInUse = true,  preEqBandCount = 10,
    mbcInUse = false,   mbcBandCount = 0,     // enable in Pro
    postEqInUse = false, postEqBandCount = 0,
    limiterInUse = true  // always — prevents BT clipping
).build()

val dsp = DynamicsProcessing(priority=0, audioSession=sessionId, config)

// Set exact frequency per band — no Gaussian mapping needed
val band = DynamicsProcessing.EqBand(enabled=true, cutoffFrequency=32f, gain=gainDb)
dsp.setPreEqBand(channel=0, band=0, band)
```

**Features:**
- True parametric EQ at exact user-defined frequencies
- Built-in Limiter — prevents clipping from layered car/phone EQ
- Multiband Compression (Pro) — professional car cabin dynamics control
- Pre-EQ and Post-EQ chains (Pro) — advanced DSP signal path

---

#### Layer 2 — Enhanced Session Detection

```kotlin
// AudioSessionManager.kt
class AudioSessionManager(context: Context) {

    // Standard mode — broadcast receiver
    private val sessionReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
            if (sessionId != -1) attachDspToSession(sessionId)
        }
    }

    // Enhanced mode — polls dump (requires DUMP permission)
    private fun pollSystemSessions() {
        // Parse output of DumpManager for active audio sessions
        // Attach DSP to any untracked session IDs found
        // Release DSP when session no longer appears in dump
    }
}
```

**Two modes exposed in Settings:**
- Standard (default): works with most music apps, no extra setup
- Enhanced (Pro): guides user through Shizuku or ADB DUMP grant, works with Spotify

---

#### Layer 3 — AutoEQ Algorithm + Car Targets (Intelligence Layer — Pro)

**Source:** MIT-licensed algorithm from jaakkopasanen/AutoEq — safe to reimplement.

**Car-specific targets (BassRide originals):**

| Target | Science basis |
|---|---|
| Harman Car 2018 | Harman research on preferred in-car listening |
| Flat | Pure reference — no coloration |
| Bass Emphasis | Flat + compensates car cabin bass rolloff below 80 Hz |
| Vocal Clarity | Mid-forward — voice/podcasts in noisy cabins |
| Road Noise Comp | 2–6 kHz boost — cuts through highway wind noise |
| Night Drive | Relaxed treble, warm bass for quiet night driving |

---

### Full Revised Data Flow

```
[USER SELECTS TARGET or AUTO-EQ button]
         ↓
┌────────────────────────────────────┐
│  Layer 3 — AutoEQ Algorithm (Pro)  │
│  AutoEqEngine.kt                   │
│  Input: Car target curve           │
│  Output: List<ParametricBand>      │
│  (freq, gain, Q per band)          │
└──────────────┬─────────────────────┘
               ↓ (or manual preset)
┌────────────────────────────────────┐
│  Layer 1 — DynamicsProcessing API  │
│  BassRideDspEngine.kt              │
│  Sets exact Hz/gain/Q per band     │
│  Limiter always on                 │
│  MBC on (Pro)                      │
└──────────────┬─────────────────────┘
               ↑ attached per session
┌────────────────────────────────────┐
│  Layer 2 — Session Detection       │
│  AudioSessionManager.kt            │
│  Standard: broadcast receiver      │
│  Enhanced: DUMP permission (Pro)   │
└──────────────┬─────────────────────┘
               ↓
   Bluetooth → Car Speaker
   🎵 Professional-grade sound
```

---

### Classes to Drop / Replace

| Old (UPDATE-001/003) | New (UPDATE-004) | Reason |
|---|---|---|
| `EqEngine` using `AudioEffect.Equalizer` | `BassRideDspEngine` using `DynamicsProcessing` | DynamicsProcessing is strictly better |
| `GaussianBandMapper.kt` | DROPPED | Not needed — DynamicsProcessing sets exact Hz directly |
| Session ID 0 attachment | Per-session via `AudioSessionManager` | Session 0 deprecated by Google |

### Classes to Keep

| Class | Status |
|---|---|
| `AutoEqEngine.kt` | ✅ Keep — algorithm unchanged, just uses MIT-confirmed source |
| `CarTargetCurves.kt` | ✅ Keep — our original car target definitions |
| `BassRideService.kt` | ✅ Keep — foreground service, now holds AudioSessionManager |
| `BluetoothReceiver.kt` | ✅ Keep — auto-preset switching on BT connect |

---

### Updated Dependencies

```kotlin
// audio-engine/build.gradle.kts
// All three layers use Android SDK only — ZERO external dependencies
// android.media.audiofx.DynamicsProcessing  (Layer 1 — API 28+, SDK built-in)
// AudioSessionManager.kt                    (Layer 2 — our code)
// AutoEqEngine.kt                           (Layer 3 — our code, MIT algorithm)
```

---

### Claude Code Implementation Prompts

**Layer 1 — DynamicsProcessing Engine:**
```
"Create BassRideDspEngine.kt in audio-engine. It wraps android.media.audiofx.DynamicsProcessing.
Configure 10-band Pre-EQ with fully parametric bands at custom frequencies. Always enable the
Limiter stage to prevent Bluetooth clipping. MBC stage disabled by default, enabled when
isProUnlocked. Expose attachToSession(sessionId: Int), applyPreset(preset: PresetEntity),
detachFromSession(sessionId: Int) as suspend functions on Dispatchers.IO. Manage multiple
concurrent sessions in a Map<Int, DynamicsProcessing>. Release all on service stop."
```

**Layer 2 — Session Detection:**
```
"Create AudioSessionManager.kt in audio-engine. Standard mode: register BroadcastReceiver
for android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION and
android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION. Enhanced mode (requires DUMP
permission): poll system service dump every 500ms for active audio sessions using
a coroutine on Dispatchers.IO. On new session found: call BassRideDspEngine.attachToSession().
On session closed: call BassRideDspEngine.detachFromSession(). Check for DUMP permission
before enabling enhanced mode. Emit SessionDetectionState as StateFlow."
```

---

## Future Updates

Add new entries below as decisions are made during development.
Format: `## [UPDATE-00X] Title` with Date, Status, and full rationale.

---