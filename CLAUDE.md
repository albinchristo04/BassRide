# CLAUDE.md — BassRide

This file guides Claude Code on architecture rules, conventions, and boundaries for this project.
Read this before modifying any file.

---

## Project Identity

| | |
|---|---|
| **App Name** | BassRide |
| **Package Name** | com.velcuri.bassride |
| **Application ID** | com.velcuri.bassride |
| **Play Store Product ID** | bassride_pro_unlock |

---

## Project Overview

BassRide is an Android equalizer app designed specifically for car Bluetooth audio.
When a user connects their phone to their car's Bluetooth, the app auto-loads a saved EQ preset
optimized for that car. Users can create per-device profiles, tune a 10-band EQ, and unlock
advanced features via a one-time Pro purchase.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin (no Java) |
| UI | Jetpack Compose (no XML layouts) |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Database | Room |
| Async | Coroutines + StateFlow |
| Audio | Android AudioEffect API (Equalizer, BassBoost, Virtualizer) |
| Billing | Google Play Billing Library v6+ |
| Min SDK | API 29 (Android 10) |
| Target SDK | Latest stable |

---

## Module Structure

```
app/                    → Entry point, Hilt setup, navigation
audio-engine/           → AudioEffect, EQ bands, presets, audio focus
bluetooth/              → BT detection, device pairing, auto-switching
ui/                     → All Compose screens and components
data/                   → Room DB, repositories, data models
billing/                → Google Play Billing, Pro unlock logic
```

---

## Architecture Rules

### General
- Every module follows Clean Architecture: `domain → data → presentation`
- No business logic inside Composables or ViewModels directly — use UseCases
- ViewModels expose `StateFlow<UiState>` only — never expose mutable state
- UseCases are single-responsibility (one public `invoke()` function)
- Repositories are interfaces in domain; implementations live in data layer

### Kotlin
- Use `data class` for all models
- Prefer `sealed class` for UI states and results
- No nullable types unless absolutely required — use `Result<T>` or sealed classes instead
- All coroutines launched in `viewModelScope` or `lifecycleScope` only
- No `GlobalScope` anywhere

### Compose
- No business logic inside Composables — only display and user events
- All Composables are stateless where possible; state hoisted to ViewModel
- Use `remember` and `derivedStateOf` correctly — avoid unnecessary recompositions
- Driver-safe UI: minimum touch target 56dp, large readable text (18sp+)

---

## Bluetooth Rules

- Use `BroadcastReceiver` with `ACTION_ACL_CONNECTED` / `ACTION_ACL_DISCONNECTED`
- On connection: read device name + MAC address, look up saved profile, auto-load EQ preset
- On disconnection: reset EQ to flat or default system state
- Request `BLUETOOTH_CONNECT` permission (API 31+) and `BLUETOOTH` (API < 31)
- Never hardcode device names — always store and match from Room DB
- Handle the case where no profile exists for a newly connected device (prompt to create one)

---

## Audio Engine Rules

BassRide uses a **three-layer engine stack**. See UPDATE-004 in UPDATE.md for full rationale.

### ⚠️ Critical Rules — Read Before Touching Audio Code

- **NEVER use `AudioEffect.Equalizer` attached to session 0** — deprecated by Google
- **NEVER use session ID 0** — deprecated; use per-session attachment only
- **ALWAYS use `DynamicsProcessing` API** — not the plain `Equalizer` subclass
- **DO NOT add `GaussianBandMapper`** — was planned in UPDATE-003, dropped in UPDATE-004
- **DO NOT add Tarsos DSP** — was planned in UPDATE-001, dropped in UPDATE-003

---

### Layer 1 — DynamicsProcessing API (Core Engine)
**Class:** `BassRideDspEngine.kt` in `audio-engine`
**API:** `android.media.audiofx.DynamicsProcessing` (API 28+ — all our supported devices)

- Configure 10-band Pre-EQ with **fully custom frequencies** (no OEM band limitations)
- Set exact parametric bands: frequency (Hz), gain (dB), Q factor per band
- **Always enable the Limiter stage** — prevents Bluetooth output clipping
- MBC (Multiband Compression) stage: disabled by default, enabled when `isProUnlocked`
- Manage multiple concurrent sessions in `Map<Int, DynamicsProcessing>`
- All `DynamicsProcessing` calls on `Dispatchers.IO` — never main thread
- Call `.release()` on every session's DSP instance when session closes
- Call `.release()` on all instances in `onDestroy` of `BassRideService`

### Layer 2 — Enhanced Session Detection
**Class:** `AudioSessionManager.kt` in `audio-engine`

**Standard mode (default):**
- Register `BroadcastReceiver` for `ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION`
- Register `BroadcastReceiver` for `ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION`
- On open: call `BassRideDspEngine.attachToSession(sessionId)`
- On close: call `BassRideDspEngine.detachFromSession(sessionId)`

**Enhanced mode (Pro — requires DUMP permission):**
- Check `android.permission.DUMP` before enabling
- Poll system service dump every 500ms on `Dispatchers.IO`
- Attach DSP to any newly detected session IDs
- Release when sessions disappear from dump
- Guide user through Shizuku (no PC) or ADB grant flow

### Layer 3 — AutoEQ Engine (Pro only)
**Class:** `AutoEqEngine.kt` in `audio-engine`
**Algorithm source:** MIT-licensed jaakkopasanen/AutoEq — reimplement independently in Kotlin

- Input: car target curve as `List<Pair<Float, Float>>` (Hz, dB breakpoints)
- Output: `List<ParametricBand>(frequency, gainDb, q)` — passed directly to Layer 1
- Greedy iterative peak-finding, up to 10 bands, terminates at <0.05 dB error
- Gate behind `isProUnlocked == true` from `BillingRepository`
- Built-in car targets: `HarmanCar, Flat, BassEmphasis, VocalClarity, RoadNoiseComp, NightDrive`

### Data Flow (mandatory reading before any audio pipeline changes)
```
Car Target / Manual Preset
         ↓
AutoEqEngine (Layer 3 — Pro)
Outputs List<ParametricBand>
         ↓
BassRideDspEngine (Layer 1)
Sets exact Hz/gain/Q via DynamicsProcessing Pre-EQ bands
Limiter always active
         ↑ per audio session
AudioSessionManager (Layer 2)
Detects Spotify / YouTube / any app sessions
Attaches DynamicsProcessing per session
         ↓
Bluetooth → Car Speaker

### EQ Target Band Frequencies (parametric — exact, not device-dependent)
```
Band 0:  32 Hz    (sub-bass)
Band 1:  64 Hz    (bass)
Band 2:  125 Hz   (upper bass)
Band 3:  250 Hz   (low-mid)
Band 4:  500 Hz   (mid)
Band 5:  1,000 Hz (upper-mid)
Band 6:  2,000 Hz (presence)
Band 7:  4,000 Hz (high-mid)
Band 8:  8,000 Hz (high)
Band 9:  16,000 Hz (air)
```

---

## Preset System

- Presets stored in Room DB (`PresetEntity`)
- Each preset linked to a `BluetoothDeviceEntity` via foreign key
- Built-in presets (Flat, Bass Boost, Vocal Clarity, Road Noise) are read-only seeds
- User presets are mutable and deletable
- Auto-switch: when BT device connects → query DB → apply linked preset if found
- Free tier: max 2 saved presets. Pro tier: unlimited

---

## Monetization Rules

- Billing state lives in `billing/` module only — never leak billing logic into other modules
- Use `BillingRepository` interface; other modules depend on it via Hilt injection
- One-time purchase product ID: `"bassride_pro_unlock"`
- Free features: 5-band EQ, 2 presets, basic Bluetooth detection
- Pro features: 10-band EQ, unlimited presets, auto-switching, road noise profiles, widget
- Always verify purchase on device via `Purchase.purchaseState == PurchaseState.PURCHASED`
- Never gate safety-related features behind Pro
- Show upgrade prompt — never block the app or force upsell on every screen

---

## Permissions Required

```xml
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```

- Request permissions at runtime with clear user-facing rationale
- If Bluetooth permission is denied, show an explanation screen — don't silently fail

---

## Foreground Service

- Run a `BassRideService` (Foreground Service) to keep EQ active when app is backgrounded
- Show a persistent notification with quick preset switcher
- Service must be started on BT connect, stopped on BT disconnect (or user toggle)
- Handle `ACTION_AUDIO_BECOMING_NOISY` to avoid audio glitches on BT drop

---

## Room Database

### Entities
- `BluetoothDeviceEntity`: id, name, macAddress, linkedPresetId
- `PresetEntity`: id, name, isBuiltIn, band0..band9 (Int, millibels), bassBoostStrength, createdAt
- `UserSettingsEntity`: id, autoSwitchEnabled, defaultPresetId, isProUnlocked (cached)

### Rules
- All DB access through Repository — no direct DAO calls from ViewModel
- Use `Flow<List<T>>` for reactive queries
- Migrations required for any schema change — never use `fallbackToDestructiveMigration` in production

---

## What NOT to Do

- Do not use `AsyncTask` — deprecated
- Do not use `LiveData` — use `StateFlow`
- Do not use View system / XML layouts — Compose only
- Do not call AudioEffect APIs on the main thread
- Do not store Pro unlock state only in memory — always re-verify with Play Billing
- Do not use `Thread.sleep()` — use coroutine `delay()`
- Do not show ads — this is a paid/freemium app with no ad network
- Do not access Bluetooth APIs without checking permission first

---

## Testing Requirements

- Unit tests for all UseCases (`/domain/usecase/`)
- Unit tests for all ViewModels using `TestCoroutineDispatcher`
- Integration tests for Room DB (use in-memory database)
- Manual test checklist for BT connect/disconnect cycle before each release

---

## File Naming Conventions

| Type | Convention | Example |
|---|---|---|
| ViewModel | `[Feature]ViewModel` | `PresetViewModel` |
| UseCase | `[Verb][Noun]UseCase` | `LoadPresetForDeviceUseCase` |
| Repository | `[Noun]Repository` | `BluetoothRepository` |
| Composable | `[Feature]Screen` / `[Name]Component` | `EqScreen`, `BandSlider` |
| Entity | `[Noun]Entity` | `PresetEntity` |