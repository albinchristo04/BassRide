# PROJECT.md — BassRide
## Android App Development Roadmap

---

## App Identity

| | |
|---|---|
| **App Name** | BassRide |
| **Package Name** | com.velcuri.bassride |
| **Application ID** | com.velcuri.bassride |
| **Platform** | Android (API 29+) |
| **Language** | Kotlin + Jetpack Compose |
| **Monetization** | Freemium — One-time Pro unlock |
| **Target Price** | Free (basic) / $4.99–$7.99 Pro |
| **Play Store Product ID** | bassride_pro_unlock |
| **Target Audience** | Android users who stream music to car via Bluetooth |
| **Play Store Category** | Music & Audio |

---

## Core Value Proposition

> *"Connect your phone to your car Bluetooth — your perfect EQ loads automatically."*

Most equalizer apps are generic. This app is built from the ground up for the car listening
environment: auto-switching presets per device, road noise compensation, a driver-safe UI,
and stable Bluetooth session tracking.

---

## Feature List

### Free Tier
- [x] 5-band equalizer (system-wide, via AudioEffect API)
- [x] 3 built-in presets: Flat, Bass Boost, Vocal Clarity
- [x] Save up to 2 custom presets
- [x] Manual Bluetooth device detection
- [x] Basic UI with preset selector

### Pro Tier (one-time unlock)
- [ ] 10-band parametric equalizer
- [ ] Unlimited saved presets
- [ ] Auto-load preset when specific car Bluetooth connects
- [ ] Per-device EQ profiles (link preset to a BT device)
- [ ] Road Noise profile (compensates for highway wind/engine noise)
- [ ] Quick-access floating widget
- [ ] Persistent foreground service (EQ stays active in background)
- [ ] Bass Booster + Virtualizer effects
- [ ] Import/export presets

---

## Phase Breakdown

---

### Phase 1 — Project Scaffold
**Goal:** Working Android project with correct structure, DI, and navigation.

**Tasks:**
- [ ] Create Android project with correct module structure
- [ ] Set up Hilt for dependency injection across all modules
- [ ] Set up Jetpack Compose navigation (NavHost)
- [ ] Create Room database with all entities and DAOs
- [ ] Create `CLAUDE.md` at project root (copy from repo root)
- [ ] Add all required permissions to `AndroidManifest.xml`
- [ ] Create placeholder screens: Home, EQ, Presets, Devices, Settings, Upgrade

**Claude Code prompts to use:**
```
"Scaffold a multi-module Android project named BassRide with package name com.velcuri.bassride.
Modules: app, audio-engine, bluetooth, ui, data, billing.
Use Hilt for DI, Jetpack Compose for UI, Room for database, Kotlin Coroutines + StateFlow.
Min SDK 29, target latest stable. Follow the architecture rules in CLAUDE.md."
```

```
"Create Room database entities for BluetoothDeviceEntity, PresetEntity, and UserSettingsEntity
as defined in CLAUDE.md. Include DAOs with Flow-based queries and a RoomDatabase class with
migration support."
```

**Done when:** App builds, launches, shows placeholder nav, DI works.

---

### Phase 2 — Bluetooth Detection Module
**Goal:** App detects car BT connect/disconnect and stores device profiles.

**Tasks:**
- [ ] `BluetoothReceiver` — BroadcastReceiver for ACL_CONNECTED / ACL_DISCONNECTED
- [ ] `BluetoothRepository` — interface + Room-backed implementation
- [ ] `GetConnectedDevicesUseCase`
- [ ] `SaveDeviceProfileUseCase`
- [ ] `LinkPresetToDeviceUseCase`
- [ ] `DevicesViewModel` + Devices screen (list of paired BT devices)
- [ ] On first detection of a new device: prompt user to create/link a profile
- [ ] Runtime permission request flow for BLUETOOTH_CONNECT

**Claude Code prompts to use:**
```
"Create a BroadcastReceiver that listens for ACTION_ACL_CONNECTED and ACTION_ACL_DISCONNECTED.
On connect, extract device name and MAC address, look up the device in Room DB, and emit a
StateFlow event for the UI. Handle BLUETOOTH_CONNECT permission for API 31+."
```

```
"Create a Compose screen showing all saved Bluetooth device profiles. Each row shows device name,
linked preset name, and a toggle for auto-switch. Tapping a device opens an edit sheet to
change the linked preset. Follow MVVM — no logic in Composables."
```

**Done when:** Phone connects to car BT → app detects it → device appears in list.

---

### Phase 3 — Audio Engine Module
**Goal:** Working EQ that processes audio output to Bluetooth.

**Tasks:**
- [ ] `EqEngine` — wraps Android `Equalizer`, `BassBoost`, `Virtualizer`
- [ ] Audio Focus listener — EQ survives app switches
- [ ] `ApplyPresetUseCase`
- [ ] `SavePresetUseCase`
- [ ] `LoadPresetsUseCase`
- [ ] Foreground service (`CarEqService`) to keep EQ alive in background
- [ ] Seed database with built-in presets on first launch
- [ ] Band level validation (clamp to device min/max millibels)

**Claude Code prompts to use:**
```
"Create an EqEngine class in the audio-engine module that wraps Android's AudioEffect Equalizer.
It should support 10 bands where available, fall back gracefully to fewer bands, attach to
audio session 0, handle audio focus changes, and expose suspend functions for applying and
releasing effects. All AudioEffect calls must run on an IO dispatcher, never main thread."
```

```
"Create a Foreground Service (CarEqService) that holds the EqEngine alive when the app is
backgrounded. The service should start when Bluetooth connects, show a persistent notification
with current preset name and a next-preset button, and stop when Bluetooth disconnects.
Use FOREGROUND_SERVICE_MEDIA_PLAYBACK type."
```

**Done when:** EQ affects audio output to car BT speakers; survives app backgrounding.

---

#```md
### Phase 4.1 — Driver-Friendly UX Enhancements
**Goal:** Make the EQ experience intuitive, safe, and fast while driving.

**Tasks:**
- [ ] Add "Simple Mode" with one-tap sound profiles:
  - Warm
  - Deep Bass
  - Vocal Clarity
  - Highway
  - Night Drive
- [ ] Add expandable "Advanced EQ" section for full manual tuning
- [ ] Add Quick Bass control slider/knob for fast adjustments
- [ ] Add "Undo Last Change" action
- [ ] Add A/B comparison toggle between current and previous preset
- [ ] Add center-detent snapping for sliders at 0 dB
- [ ] Add subtle haptic feedback on:
  - preset switch
  - slider center snap
  - toggles
- [ ] Add high-contrast driving mode for daylight visibility
- [ ] Add oversized touch mode with larger controls
- [ ] Add visual EQ curve graph synced with sliders
- [ ] Add live frequency response preview
- [ ] Add "Reset to Flat" quick action
- [ ] Add onboarding tooltip explaining EQ basics

**Claude Code prompts to use:**
```

"Create a Simple Mode UI above the advanced EQ controls with large one-tap sound profiles:
Warm, Deep Bass, Vocal Clarity, Highway, and Night Drive. Selecting a profile should apply
a predefined EQ curve instantly. Include smooth transitions between profiles and highlight
the active mode."

```
```

"Create an interactive EQ response graph for the EqScreen. The graph should visually reflect
the current slider positions in real time. Dragging graph points should update the linked EQ
band sliders bidirectionally. Use Compose Canvas APIs."

```

**Done when:** Users can quickly improve sound without understanding EQ bands.

---

### Phase 4.2 — Real-Time Audio Stability
**Goal:** Ensure smooth EQ updates without audio glitches or excessive CPU usage.

**Tasks:**
- [ ] Add preview mode while dragging sliders
- [ ] Commit final EQ values on drag release
- [ ] Add optional "Live Apply" toggle in settings
- [ ] Debounce rapid EQ updates intelligently
- [ ] Prevent excessive AudioEffect calls during fast dragging
- [ ] Add fallback handling for unsupported bands/effects
- [ ] Add automatic recovery if AudioEffect session dies
- [ ] Add EQ inactive detection and recovery UI state
- [ ] Add service reconnection handling
- [ ] Add volume compensation for boosted presets
- [ ] Clamp unsafe EQ amplification combinations

**Claude Code prompts to use:**
```

"Optimize EqScreen slider interaction for smooth audio playback. While dragging, apply a
temporary preview EQ state with throttled updates. On drag release, commit the final EQ
values to EqEngine. Avoid excessive AudioEffect calls and ensure smooth playback on lower-end
devices."

```
```

"Implement AudioEffect recovery handling in EqEngine. Detect when the Equalizer session becomes
invalid or released unexpectedly by the system. Automatically recreate the session, reapply
the active preset, and emit recovery state updates to the UI via StateFlow."

```

**Done when:** EQ changes feel smooth, stable, and reliable across devices.

---

### Phase 4.3 — Preset Intelligence & Personalization
**Goal:** Make presets feel smart and personalized to the user's car environment.

**Tasks:**
- [ ] Add preset categories:
  - Music
  - Podcast
  - Bass Heavy
  - Highway
  - Voice
- [ ] Add emotional preset naming system
- [ ] Add preset icons/thumbnails
- [ ] Add first-time car setup flow:
  - Sedan
  - SUV
  - Hatchback
  - Truck
- [ ] Seed recommended EQ curves based on vehicle type
- [ ] Add preset favorites/pinning
- [ ] Add recently used presets row
- [ ] Add smart preset suggestions after repeated usage
- [ ] Add optional adaptive volume balancing
- [ ] Add import/export sharing with QR or JSON

**Claude Code prompts to use:**
```

"Create a first-time car setup wizard. Ask the user what type of vehicle they drive:
Sedan, SUV, Hatchback, or Truck. Based on selection, apply recommended starter EQ presets
optimized for that cabin style."

```
```

"Create a smart preset system that tracks recently used presets and displays them in a
horizontal quick-access row above the EQ controls. Favorite presets should appear first and
persist between launches."

```

**Done when:** Presets feel personalized and immediately useful for new users.

---

### Phase 4.4 — Safe Driving Interaction Layer
**Goal:** Reduce driver distraction and optimize usability while driving.

**Tasks:**
- [ ] Add driving-safe UI mode
- [ ] Simplify controls while driving:
  - preset switching
  - quick bass adjustment
  - enable/disable EQ
- [ ] Disable fine slider tuning while moving (optional)
- [ ] Add larger touch targets during driving mode
- [ ] Add voice-friendly preset names
- [ ] Add notification quick actions:
  - next preset
  - previous preset
  - toggle EQ
- [ ] Add Quick Settings tile for EQ toggle
- [ ] Add lock screen controls support
- [ ] Add optional automatic driving mode activation
- [ ] Add reduced-animation mode for safer interaction

**Claude Code prompts to use:**
```

"Create a Driving Mode UI for EqScreen that simplifies the interface while the device is in
motion. Hide advanced EQ sliders and show only large preset buttons, bass controls, and an
EQ enable toggle. All controls should have oversized touch targets."

```
```

"Add notification quick actions for EQ control. The foreground notification should include:
Next Preset, Previous Preset, and Toggle EQ actions. Actions should work without reopening
the app."

```

**Done when:** The app can be safely used with minimal distraction while driving.
```

### Phase 5 — Auto-Switch & Device Profiles
**Goal:** EQ auto-loads correct preset when car BT connects.

**Tasks:**
- [ ] Connect Phase 2 BT detection to Phase 3 audio engine
- [ ] `AutoSwitchUseCase` — on BT connect, find linked preset, apply it
- [ ] Notify user via notification when auto-switch happens
- [ ] `DeviceProfileScreen` — create/edit a profile, link it to a preset
- [ ] Settings toggle: enable/disable auto-switch globally
- [ ] Handle edge case: BT connects before app service starts

**Claude Code prompts to use:**
```
"Connect the BluetoothReceiver to the CarEqService. When ACTION_ACL_CONNECTED fires, pass the
MAC address to AutoSwitchUseCase which queries Room for a linked preset. If found and
auto-switch is enabled, apply it via EqEngine and update the notification. If no preset is
linked, show a notification prompting the user to set one up."
```

**Done when:** Connect to car BT → correct EQ preset loads automatically, notification confirms it.

---

### Phase 6 — Billing & Pro Unlock
**Goal:** Seamless one-time Pro purchase via Google Play.

**Tasks:**
- [ ] Set up Google Play Billing Library v6+
- [ ] `BillingRepository` — connect to Play, query product, launch purchase flow
- [ ] `VerifyPurchaseUseCase` — validate purchase state on every app launch
- [ ] `ProStatusViewModel` — exposes `isProUnlocked: StateFlow<Boolean>`
- [ ] `UpgradeScreen` — feature comparison list, price, purchase button
- [ ] Gate Pro features in UI using `isProUnlocked` state
- [ ] Restore purchases support
- [ ] Test with Play Console test accounts before release

**Claude Code prompts to use:**
```
"Implement Google Play Billing Library v6 in the billing module. Create a BillingRepository
that connects to the Play Billing client, queries the one-time product 'bassride_pro_unlock',
launches the purchase flow, and verifies PURCHASED state. Expose isProUnlocked as
StateFlow<Boolean> via Hilt. Cache result in Room UserSettingsEntity but always re-verify
on launch."
```

```
"Create an UpgradeScreen in Jetpack Compose. Show a feature comparison between Free and Pro.
Include a prominent purchase button that triggers the billing flow. Handle loading, error,
and already-purchased states. Include a 'Restore Purchase' text button."
```

**Done when:** Purchase flow works end-to-end in Play Console testing; Pro features unlock correctly.

---

### Phase 7 — Polish & Launch Prep
**Goal:** App is stable, polished, and ready for Play Store submission.

**Tasks:**
- [ ] Onboarding flow (3 screens: what the app does, BT permission, create first preset)
- [ ] Empty states for all screens
- [ ] Error handling for all AudioEffect failures (some devices don't support all effects)
- [ ] Dark mode support (Compose Material3 theming)
- [ ] Accessibility: content descriptions on all interactive elements
- [ ] ProGuard/R8 rules for Billing and AudioEffect
- [ ] Play Store listing: screenshots, short/long description, feature graphic
- [ ] Privacy Policy (required for Play Store — Bluetooth permission needs explanation)
- [ ] Final testing checklist (see below)

**Claude Code prompts to use:**
```
"Create a 3-step onboarding flow using Compose. Step 1: app overview with illustration.
Step 2: request BLUETOOTH_CONNECT permission with clear rationale. Step 3: create first
preset or pick a built-in one. Show only on first launch (track in UserSettingsEntity).
Use HorizontalPager with dot indicators."
```

**Done when:** App passes all checklist items below and is submitted to Play Store.

---

## Pre-Launch Testing Checklist

### Bluetooth
- [ ] Connect to car BT → correct preset auto-loads
- [ ] Disconnect from car BT → EQ resets to default
- [ ] Connect a new unknown device → prompt to create profile appears
- [ ] BT permission denied → explanation screen shown, not crash
- [ ] App backgrounded → EQ still active (foreground service running)

### Audio
- [ ] EQ affects output on BT connection (not just phone speakers)
- [ ] Band sliders update EQ in real time
- [ ] EQ survives switching between music apps (Spotify → YouTube Music)
- [ ] EQ survives incoming phone call then resuming music
- [ ] Device with fewer than 10 bands → app doesn't crash, shows available bands only

### Billing
- [ ] Free tier correctly limits to 5 bands and 2 presets
- [ ] Pro unlock enables all features
- [ ] Restore Purchase correctly re-unlocks Pro
- [ ] Killing and relaunching app re-verifies Pro status correctly
- [ ] Purchase failure handled gracefully (no stuck loading state)

### General
- [ ] No crashes on Android 10, 12, 13, 14 (test on emulators)
- [ ] Dark mode renders correctly
- [ ] Back navigation works on all screens
- [ ] App doesn't drain battery excessively (check with Battery Historian)

---

## Play Store Listing Copy (Draft)

**Short description (80 chars):**
> Auto-load your perfect car EQ every time Bluetooth connects.

**Long description:**
> BassRide is the only equalizer app built specifically for car audio over Bluetooth.
>
> Most EQ apps are generic. BassRide detects which car you've connected to and automatically
> loads your saved sound profile — so every drive sounds exactly the way you tuned it.
>
> **Key Features:**
> - 10-band parametric equalizer for precise sound control
> - Auto-switch presets per Bluetooth device
> - Road noise compensation profiles
> - Driver-safe UI with large controls
> - Runs in background — EQ stays active when the app is closed
> - One-time Pro unlock — no subscriptions, no ads
>
> Works with any car stereo connected via Bluetooth.

---

## Folder Structure (Final)

```
BassRide/
├── CLAUDE.md                      ← AI instructions (this repo's rules)
├── PROJECT.md                     ← This file
├── app/
│   ├── src/main/
│   │   ├── com/velcuri/bassride/
│   │   │   ├── BassRideApplication.kt
│   │   │   ├── MainActivity.kt
│   │   │   └── navigation/AppNavGraph.kt
├── audio-engine/
│   ├── com/velcuri/bassride/audio/
│   │   ├── EqEngine.kt
│   │   ├── BassRideService.kt
│   │   └── domain/usecase/
├── bluetooth/
│   ├── com/velcuri/bassride/bluetooth/
│   │   ├── BluetoothReceiver.kt
│   │   ├── BluetoothRepository.kt
│   │   └── domain/usecase/
├── ui/
│   ├── com/velcuri/bassride/ui/
│   │   ├── eq/EqScreen.kt
│   │   ├── presets/PresetsScreen.kt
│   │   ├── devices/DevicesScreen.kt
│   │   ├── upgrade/UpgradeScreen.kt
│   │   ├── onboarding/OnboardingScreen.kt
│   │   └── components/
├── data/
│   ├── com/velcuri/bassride/data/
│   │   ├── db/BassRideDatabase.kt
│   │   ├── entity/
│   │   └── repository/
└── billing/
    ├── com/velcuri/bassride/billing/
    │   ├── BillingRepository.kt
    │   └── domain/usecase/
```
