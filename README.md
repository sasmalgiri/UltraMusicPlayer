# 🎵 UltraMusic Player

**Professional Music Player with Industry-Leading Audio Manipulation**

A powerful Android music player that exceeds all competition with advanced speed and pitch control, designed for musicians, DJs, music learners, and audio enthusiasts.

---

## 🚀 Features That Exceed Competition

### Speed Control - Extended Range
| Feature | Competition | UltraMusic |
|---------|-------------|------------|
| Speed Range | 0.25x - 4.0x | **0.05x - 10.0x** |
| Precision | 0.05x steps | **0.01x steps** |
| Quality at Extremes | Degrades | **Preserved** |

### Pitch Control - 3 Octaves!
| Feature | Competition | UltraMusic |
|---------|-------------|------------|
| Pitch Range | ±12 semitones | **±36 semitones** |
| Fine Tuning | Semitone steps | **0.01 cent precision** |
| Formant Control | Basic/None | **Full preservation** |

### Audio Quality
- **Phase Vocoder** - Frequency domain processing for highest quality
- **Multi-Resolution Processing** - Different FFT sizes for bass/mids/highs
- **Formant Preservation** - Natural vocals even at extreme pitch shifts
- **Transient Detection** - Preserved drum attacks and percussion

---

## 📱 App Features

### Library
- 🗂️ **Multi-Storage Support** - Internal + SD Card
- 🎤 **Voice Search** - Search by speaking
- 📁 **Folder Browsing** - Navigate by directory
- 🔍 **Smart Search** - By title, artist, or album

### Now Playing
- 📊 **Waveform Display** - Visual audio representation
- 🔁 **A-B Loop** - Practice sections with precision
- 🎛️ **Extended Controls** - Speed, pitch, formant
- ⚡ **Quick Presets** - Nightcore, Slowed, Vaporwave

### Quality Modes
- **Ultra High** - Best quality, studio-grade
- **High** - Great quality, recommended
- **Balanced** - Good quality, optimized performance
- **Voice** - Optimized for speech/vocals
- **Instrument** - Optimized for instruments
- **Percussion** - Preserved transients

---

## 🛠️ Technical Architecture

### Native Audio Engine (C++)
```
├── AudioEngine.cpp      - Main engine with Oboe integration
├── PhaseVocoder.cpp     - High-quality time/pitch processing
├── TimeStretcher.cpp    - Extended speed control (0.05x-10x)
├── PitchShifter.cpp     - Extended pitch control (±36 semitones)
├── FormantPreserver.cpp - Natural vocal quality
├── FFTProcessor.cpp     - SIMD-optimized FFT (NEON/SSE)
└── JNIBridge.cpp        - Kotlin ↔ C++ interface
```

### Android App (Kotlin)
```
├── audio/
│   └── NativeAudioEngine.kt  - JNI bindings
├── data/
│   ├── model/                - Data classes
│   └── repository/           - Music scanning
├── ui/
│   ├── screens/              - Jetpack Compose screens
│   ├── components/           - Reusable UI components
│   └── theme/                - Material 3 theming
├── viewmodel/                - MVVM ViewModels
└── utils/                    - Voice search, utilities
```

### Key Technologies
- **Oboe** - Google's low-latency audio library
- **Jetpack Compose** - Modern declarative UI
- **Hilt** - Dependency injection
- **Media3** - Audio decoding
- **Room** - Local database
- **Coroutines + Flow** - Reactive programming

---

## 🔧 Building the Project

### Prerequisites
- Android Studio Hedgehog or later
- Android NDK r25+
- JDK 17
- CMake 3.22+

### Build Steps
```bash
# Clone the repository
git clone https://github.com/yourusername/UltraMusicPlayer.git

# Open in Android Studio
# Sync Gradle
# Build → Make Project

# Or build from command line
./gradlew assembleDebug
```

### Running on Device
1. Enable Developer Options on your Android device
2. Enable USB Debugging
3. Connect device via USB
4. Run → Select your device

---

## 📊 Algorithm Details

### Phase Vocoder
Our phase vocoder uses:
- **STFT** with configurable FFT sizes (2048-8192)
- **Phase locking** to reduce phasiness
- **Transient detection** for percussion preservation
- **Vertical phase coherence** for stereo content

### Multi-Resolution Processing
For optimal quality, we split the audio into frequency bands:
- **Low band (< 250 Hz)**: 8192 FFT - better frequency resolution
- **Mid band (250-4000 Hz)**: 4096 FFT - balanced
- **High band (> 4000 Hz)**: 2048 FFT - better time resolution

### Formant Preservation
Uses **True Envelope** estimation:
1. Extract spectral envelope (formants)
2. Separate fine structure (pitch harmonics)
3. Shift pitch while keeping envelope
4. Recombine for natural sound

---

## 📱 Screenshots

```
┌─────────────────────────┐
│  🎵 Now Playing         │
│  ═══════════════════    │
│  [Waveform Display]     │
│                         │
│  Speed: 1.35x           │
│  ●━━━━━━━━━━━━━━━━━━━━  │
│                         │
│  Pitch: +2.5 st         │
│  ━━━━━━━━●━━━━━━━━━━━━  │
│                         │
│  [Nightcore] [Slowed]   │
│                         │
│    ⏮️   ▶️   ⏭️        │
└─────────────────────────┘
```

---

## 📄 License

This project is for educational purposes. 

For commercial use:
- **Rubber Band Library** requires commercial license
- Other components may have different licenses

---

## 🤝 Contributing

Contributions welcome! Please:
1. Fork the repository
2. Create feature branch
3. Submit pull request

---

## 📞 Support

- 📧 Email: support@ultramusic.app
- 🐛 Issues: GitHub Issues
- 💬 Discussions: GitHub Discussions

---

**Made with ❤️ for musicians and audio enthusiasts**
