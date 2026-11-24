# MONO // BEACON - Android Application

## Overview
MONO // BEACON is a native Android application designed to locate a lost phone buried in bedding by triggering a maximum-volume alarm, flashlight strobe, and vibration pattern, even when the phone is on Silent or Do Not Disturb.

**Target Devices:** POCO C71 / HyperOS / MIUI (and all Android 8.0+ devices)

**Architecture:** MVVM with Jetpack Compose UI

**Min SDK:** 26 (Android 8.0) | **Target SDK:** 34

---

## 🎨 Visual Design System

### Color Palette (Strict B&W)
- **AMOLED Black:** `#000000` - Background
- **Off-Black:** `#0D0D0D` - Elevated Surface
- **Hairline Border:** `#262626` - 1dp borders
- **Pure White:** `#FFFFFF` - Primary Ink/Active Buttons
- **Light Gray:** `#8A8A8A` - Secondary Text/Muted Labels
- **Dark Charcoal:** `#333333` - Disabled/Inactive Elements

### Typography
- Geometric sans-serif hierarchy with `letterSpacing = 1.5.sp` on uppercase headers
- Bold high-contrast numerals
- Font: Roboto (default Android)

### Animations
- Custom `Modifier.scrollFadeIn` - fade in (0f -> 1f) and translate upward (24f -> 0f)
- 300ms transitions with `FastOutSlowInEasing`
- No bouncy, glassy, or colorful effects

---

## 📱 Target Device Optimization (POCO / HyperOS / MIUI)

### Zero-Drain Passive Architecture
- No persistent wake locks or background polling loops
- Relies on OS-level event-driven triggers only:
  - Firebase Cloud Messaging high-priority data messages
  - Android `SMS_RECEIVED` system broadcast
- Idle battery drain near 0%

### POCO / HyperOS Setup Helper
The app includes a dedicated setup step that detects Xiaomi/POCO devices and provides direct intent buttons to:
1. **Open MIUI/HyperOS Autostart settings** - Set Battery Saver to "No restrictions"
2. **Enable DND exceptions** for the beacon service
3. **Grant camera permission** for flashlight strobe

**Detection:** `Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)`

---

## ⚙️ Settings & Configuration

### On-Bech (Her Phone) Settings Screen
Accessible via bottom navigation → **Settings** tab:

| Setting | Default | Description |
|---------|---------|-------------|
| **Listening Mode** | `Scheduled Window` | `Scheduled Window` / `24/7 Always Active` / `Paused` |
| **Start Time** | `08:00 AM` | Custom editable time picker (2 taps) |
| **End Time** | `11:00 PM` | Custom editable time picker (2 taps) |
| **Secret SMS Keyword** | `RINGNOW` | Editable keyword for SMS commands (case-insensitive) |
| **Alarm Duration** | `2 mins` | Options: 1 min, 2 min, 5 min, 10 min |
| **LED Strobe** | `On` | Toggle rear camera flashlight strobe (every 400ms) |

### SMS Commands (Offline - No Internet Required)
Works without Wi-Fi/mobile data. All commands are case-insensitive.

| Command | Effect |
|---------|--------|
| `RINGNOW` (or custom keyword) | Triggers alarm immediately, bypasses schedule |
| `BEACON ON` / `BEACON 24/7` | Sets Always Active mode, saves to DataStore |
| `BEACON WINDOW HH:MM-HH:MM` | Updates scheduled times (e.g., `BEACON WINDOW 09:00-22:00`) |
| `BEACON OFF` | Pauses scheduled listening (RINGNOW still works) |

### Online (Partner Dashboard via Firebase)
- **Finder Dashboard** giant button: **"RING NOW"** - Always forces alarm, bypasses schedule
- **Remote Schedule Control** - View current window, push updates remotely
- Firebase Realtime DB + FCM for remote configuration

---

## 🔧 Firebase Setup

### 1. Create Firebase Project
1. Go to [console.firebase.google.com](https://console.firebase.google.com)
2. Create new project: "MONO // BEACON"
3. Register Android app: `package_name = "com.monobeacon.monobeacon"`
   - Debug signing certificate: `SHA-1 debug`

### 2. Add Firebase Configuration
1. Download `google-services.json`
2. Place in: `mono-beacon/app/google-services.json`

### 3. Enable Required Services
- **Firebase Cloud Messaging** (FCM) - For high-priority messages
- **Firebase Realtime Database** - For remote schedule control
- **Firebase Instance ID** - For token management

### 4. Add to AndroidManifest
The `google-services.gradle` plugin automatically adds the required services.

### 5. Optional: Test Device Configuration
- Enable "High priority messages can bypass Do Not Disturb" in test device settings
- Ensure app is whitelisted in battery optimizer

---

## 📱 SMS Command Cheat Sheet

### From Partner's Phone (any carrier, no data needed)

**Emergency Trigger:**
```
RINGNOW
```
- Immediately rings maximum-volume alarm
- Bypasses scheduled window restrictions
- Triggers flashlight strobe and vibration

**Mode Control:**
```
BEACON ON
```
- Sets 24/7 Always Active mode
- Saves to phone's storage

```
BEACON WINDOW 09:00-22:00
```
- Sets scheduled window 9AM - 10PM
- Updates remote Firebase status when online

```
BEACON OFF
```
- Pauses scheduled listening
- Still allows explicit `RINGNOW` override

**Customization:** Change the default keyword `RINGNOW` in Settings → Custom Secret SMS Keyword

---

## 📦 Build & Installation

### Prerequisites
- Android Studio 2024.3.2+
- JDK 17
- pnpm (recommended) or npm
- Firebase project configured

### Building from Source
```bash
# Clone repository
git clone <repository-url>
cd mono-beacon

# Install dependencies
pnpm install

# Build debug APK
pnpm android:build

# Or using Gradle directly
./gradlew assembleDebug
```

### APK Location
- `mono-beacon/app/build/outputs/apk/debug/app-debug.apk`

### GitHub Actions
Push to `main` or `master` branch automatically compiles debug APK.
- Artifacts uploaded as `mono-beacon-debug-apk`
- Download from GitHub Actions tab

---

## 🛠️ Development Notes

### Architecture
```
monobeacon/
├── ui/theme/           # Monochrome B&W theming
├── ui/compose/         # Custom modifiers & UI components
├── ui/screen/          # Home, Settings, AlarmActiveActivity
├── service/            # AlarmService, Firebase services
├── receiver/           # SMS, FCM, Instance ID receivers
└── data/               # DataStore persistence
```

### Key Design Decisions
1. **Strict monochrome B&W** - No colors beyond black and white
2. **Foreground Service only** - No background polling, near 0% drain
3. **SMS-first approach** - Works offline without internet
4. **POCO/HyperOS friendly** - Autostart detection and setup
5. **Alarm override engine** - Bypasses Silent Mode and DND via AudioManager

### Known Limitations
- Camera permission required for flashlight (one-time prompt)
- Foreground service notification shown during active alarm
- Alarm stops after max duration (configurable 1-10 min)
- Requires Android 8.0+ (MIN_SDK = 26)

---
Built with ❤️ for AntiGem - MONO // BEACON 2026
