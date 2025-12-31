# UltraMusic Player 🎵⚔️

## The Ultimate Sound Battle Music Player for Android

**Version:** 1.0.0 PRODUCTION  
**Platform:** Android 8.0+ (API 26+)  
**Architecture:** Jetpack Compose + Media3/ExoPlayer + Hilt

---

## 🌟 Features That EXCEED Competition

### Core Music Player
- ✅ **Speed Control:** 0.05x to 10.0x (industry-leading range)
- ✅ **Pitch Control:** -36 to +36 semitones
- ✅ **Audio Presets:** Nightcore, Slowed, Vaporwave, Chipmunk, Deep Voice
- ✅ **Gapless Playback:** Seamless track transitions
- ✅ **Smart Search:** Fuzzy matching, typo tolerance
- ✅ **Folder Browser:** Navigate by directory structure
- ✅ **Background Playback:** Full notification controls

### Sound Battle System (UNIQUE)
- 🔥 **10-Band Equalizer:** Precision frequency control
- 🔥 **Bass Boost:** 0-1000 intensity
- 🔥 **Loudness Enhancer:** Competitive volume advantage
- 🔥 **Battle Presets:** BASS_CANNON, CLARITY_CUT, MAXIMUM_IMPACT, OUTDOOR/INDOOR
- 🔥 **Real-Time Visualization:** Live EQ display

### AI Battle Features (EXCLUSIVE)
- 🤖 **Real BPM Detection:** Actual onset detection algorithms
- 🤖 **Key Detection:** Krumhansl-Schmuckler algorithm
- 🤖 **Opponent Analysis:** Real-time audio capture & analysis
- 🤖 **Auto-Counter EQ:** Automatically exploits opponent weaknesses
- 🤖 **Auto-Volume Match:** Always stay louder
- 🤖 **Momentum Tracking:** Who's winning the battle
- 🤖 **Attack Opportunity Detection:** Phone vibrates on perfect moments
- 🤖 **AI Song Suggestions:** BPM/Key-matched counter songs

### Production-Grade Core
- 🛡️ **Error Handling:** Comprehensive crash protection
- 🛡️ **Crash Reporting:** Automatic log collection
- 🛡️ **Performance Monitoring:** Operation timing
- 🛡️ **Song Fingerprinting:** ACRCloud integration ready

---

## 📊 Project Statistics

| Metric | Count |
|--------|-------|
| Kotlin Files | 47 |
| Lines of Code | 23,680+ |
| UI Screens | 11 |
| Audio Processing Classes | 12 |
| AI/Intelligence Classes | 7 |

---

## 🏗️ Architecture

```
com.ultramusic.player/
├── core/                    # Production-grade systems
│   ├── RealAudioAnalyzer    # BPM/Key/Energy detection (FFT-based)
│   ├── SongFingerprintService  # ACRCloud integration
│   ├── ProductionBattleAI   # Intelligent battle system
│   └── AppErrorHandler      # Crash reporting & logging
├── audio/                   # Audio processing
│   ├── MusicController      # Media3/ExoPlayer playback
│   ├── AudioBattleEngine    # EQ/Bass/Loudness effects
│   ├── ActiveBattleSystem   # Real-time battle logic
│   ├── CrowdAnalyzer        # Crowd energy detection
│   └── FrequencyWarfare     # Tactical EQ attacks
├── ai/                      # AI systems
│   ├── CounterSongEngine    # Counter song recommendations
│   └── RAGCounterSongGuide  # RAG implementation guide
├── data/                    # Data layer
│   ├── MusicRepository      # MediaStore scanning
│   ├── FolderRepository     # Directory structure
│   └── SmartPlaylistManager # Playlist management
├── ui/                      # User interface
│   ├── screens/             # 11 Compose screens
│   ├── components/          # Reusable UI components
│   └── MainViewModel        # State management
└── di/                      # Dependency injection
    └── AppModule            # Hilt module
```

---

## 🚀 Getting Started

### Option 1: GitHub + VS Code (CI/CD Build)

1. **Create GitHub Repository**
   ```bash
   # Extract the ZIP and initialize git
   cd UltraMusicPlayer
   git init
   git add .
   git commit -m "Initial commit"
   
   # Create repo on GitHub, then:
   git remote add origin https://github.com/YOUR_USERNAME/UltraMusicPlayer.git
   git branch -M main
   git push -u origin main
   ```

2. **GitHub Actions will automatically:**
   - ✅ Build the APK
   - ✅ Run lint checks
   - ✅ Upload artifacts

3. **Download APK:**
   - Go to **Actions** tab → Latest build → **Artifacts**
   - Download `UltraMusicPlayer-debug.apk`
   - Install on your Android device

### Option 2: VS Code Local Development

1. **Install Extensions:**
   - Kotlin Language
   - Gradle for Java

2. **Set up Android SDK:**
   ```bash
   export ANDROID_HOME=$HOME/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
   ```

3. **Build from terminal:**
   ```bash
   cd UltraMusicPlayer
   chmod +x gradlew
   ./gradlew assembleDebug
   ```

4. **APK location:** `app/build/outputs/apk/debug/`

### Option 3: Android Studio (Recommended)

### Requirements
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- Physical Android device (recommended for audio testing)

### Build Steps

1. **Clone/Extract** the project
2. **Open** in Android Studio
3. **Sync** Gradle (wait for dependencies)
4. **Build** → Make Project
5. **Run** on device

### Permissions Required
```xml
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.INTERNET" />
```

---

## 🎯 Competitive Advantages

| Feature | Poweramp | Neutron | PlayerPro | **UltraMusic** |
|---------|----------|---------|-----------|----------------|
| Speed Range | 0.5x-2x | 0.5x-2x | 0.5x-2x | **0.05x-10x** |
| Pitch Range | ±12 | ±12 | ±12 | **±36** |
| Battle Mode | ❌ | ❌ | ❌ | **✅** |
| AI Counter | ❌ | ❌ | ❌ | **✅** |
| BPM Detection | ❌ | ❌ | ❌ | **✅** |
| Key Detection | ❌ | ❌ | ❌ | **✅** |
| Opponent Analysis | ❌ | ❌ | ❌ | **✅** |

---

## 📱 Supported Audio Formats

- MP3, AAC/M4A, FLAC, WAV, OGG, OPUS
- All formats supported by Android MediaCodec

---

## 🧪 Testing

See `TESTING_CHECKLIST.md` for comprehensive pre-release testing guide.

---

## 📄 License

Copyright © 2024. All rights reserved.

### Third-Party Licenses
- Media3/ExoPlayer: Apache 2.0
- Jetpack Compose: Apache 2.0
- Hilt: Apache 2.0
- Material3: Apache 2.0

---

**Built with ❤️ for Sound Battle Champions**
