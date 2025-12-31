# 🔧 Build Instructions for UltraMusic Player

## Quick Start (Recommended)

### Step 1: Extract and Open
1. Extract `UltraMusicPlayer.zip`
2. Open Android Studio
3. Click **File → Open** and select the `UltraMusicPlayer` folder

### Step 2: Fix Gradle Wrapper (If prompted)
When Android Studio opens the project, it may show:
> "Could not find gradle-wrapper.jar"

Click **OK** or **Fix** - Android Studio will download it automatically.

Alternatively, run this in Terminal:
```bash
cd UltraMusicPlayer
./gradlew wrapper
```

### Step 3: Sync Project
- Click **File → Sync Project with Gradle Files**
- Or click the elephant icon with refresh arrow in toolbar

### Step 4: Install NDK & CMake (If prompted)
Android Studio may prompt to install:
- **NDK (Side by side)** - Required for native audio engine
- **CMake** - Required for building C++ code

Click **Install** when prompted, or go to:
**Tools → SDK Manager → SDK Tools** and check:
- ✅ NDK (Side by side)
- ✅ CMake

### Step 5: Build & Run
1. Connect Android device (or start emulator)
2. Click **Run ▶️**

---

## Troubleshooting

### Problem: "Gradle sync failed"

**Solution 1: Update Gradle**
```
File → Project Structure → Project → Gradle Version: 8.5
```

**Solution 2: Invalidate Caches**
```
File → Invalidate Caches → Invalidate and Restart
```

### Problem: "NDK not found"

**Solution:**
1. Go to **Tools → SDK Manager → SDK Tools**
2. Check **NDK (Side by side)**
3. Click **Apply**

### Problem: "CMake not found"

**Solution:**
1. Go to **Tools → SDK Manager → SDK Tools**
2. Check **CMake**
3. Click **Apply**

### Problem: Native build failing

**Quick Fix - Disable Native Build:**
1. Rename `app/build.gradle.kts` to `app/build.gradle.kts.native`
2. Rename `app/build.gradle.kts.no-native` to `app/build.gradle.kts`
3. Sync project

This uses pure Java audio processing (SonicSpeedProcessor) instead of native C++.

---

## Version Requirements

| Component | Version |
|-----------|---------|
| Android Studio | Hedgehog (2023.1.1) or newer |
| Gradle | 8.5 |
| Android Gradle Plugin | 8.2.2 |
| Kotlin | 1.9.22 |
| JDK | 17 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

---

## Project Structure

```
UltraMusicPlayer/
├── app/
│   ├── src/main/
│   │   ├── java/com/ultramusic/player/
│   │   │   ├── audio/          # Audio engines
│   │   │   ├── ui/             # Compose screens
│   │   │   ├── data/           # Data models
│   │   │   ├── di/             # Dependency injection
│   │   │   ├── ai/             # AI features
│   │   │   └── core/           # Core utilities
│   │   ├── cpp/                # Native C++ code
│   │   │   ├── soundtouch/     # SoundTouch library
│   │   │   └── *.cpp           # Battle audio engine
│   │   └── res/                # Resources
│   └── build.gradle.kts        # App build config
├── build.gradle.kts            # Root build config
├── settings.gradle.kts         # Project settings
└── gradle/wrapper/             # Gradle wrapper
```

---

## After Successful Build

The app will:
1. Request audio permissions
2. Scan your music library
3. Show songs in the home screen
4. Allow playback with speed (0.05x-10x) and pitch (±36 semitones) control

Enjoy! 🎵
