# UltraMusic Player - Complete Project Blueprint

## Overview
UltraMusic Player is a premium Android music player with revolutionary **Sound Battle** features for DJ competitions. Built with Jetpack Compose, Kotlin, and native audio processing.

---

## App Architecture

### Technology Stack
| Component | Technology |
|-----------|------------|
| UI Framework | Jetpack Compose (Material 3) |
| Language | Kotlin |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Audio Engine | SoundTouch (Native C++) |
| State Management | Kotlin Flow + StateFlow |
| Navigation | Jetpack Navigation Compose |
| Database | Room + SharedPreferences |

---

## Screen Navigation Map

```
App Launch
    │
    ├─► Permission Screen (if needed)
    │       │
    │       └─► Scanning Screen
    │               │
    └───────────────┴─► HomeScreen (Main Hub)
                            │
                            ├─► All Songs Tab
                            ├─► Artists Tab
                            ├─► Albums Tab
                            ├─► Folders Tab ──► FolderBrowserScreen
                            │
                            ├─► NowPlayingScreen (Full Player)
                            │       ├─► Speed/Pitch Controls
                            │       ├─► A-B Loop
                            │       ├─► Waveform View
                            │       └─► EQ/Presets
                            │
                            ├─► EasyPlayerScreen (Simple Player)
                            │       └─► Smart Playlist Queue
                            │
                            ├─► SmartPlaylistScreen
                            │       ├─► Party Mix
                            │       ├─► Chill Vibes
                            │       ├─► Workout Energy
                            │       └─► Focus Flow
                            │
                            ├─► VoiceSearchScreen
                            │
                            └─► Battle Mode ──► BattleHQScreen
                                                    │
                                                    ├─► INTEL Tab
                                                    │     └─► Opponent Analysis
                                                    │
                                                    ├─► ARSENAL Tab
                                                    │     ├─► BattleLibraryScreen
                                                    │     └─► BattleArmoryScreen
                                                    │
                                                    ├─► VENUE Tab
                                                    │     └─► VenueProfiler
                                                    │
                                                    └─► CONTROLS Tab
                                                          ├─► Battle Modes
                                                          ├─► EQ Controls
                                                          └─► Auto Features
```

---

## All Screens (15 Total)

### Core Screens
| # | Screen | File | Purpose |
|---|--------|------|---------|
| 1 | HomeScreen | `HomeScreen.kt` | Main library with tabs |
| 2 | NowPlayingScreen | `NowPlayingScreen.kt` | Full-featured player |
| 3 | EasyPlayerScreen | `EasyPlayerScreen.kt` | Simple playlist player |
| 4 | FolderBrowserScreen | `FolderBrowserScreen.kt` | File browser |
| 5 | VoiceSearchScreen | `VoiceSearchScreen.kt` | Voice search |
| 6 | SmartPlaylistScreen | `SmartPlaylistScreen.kt` | AI playlists |
| 7 | EnhancementListScreen | `EnhancementListScreen.kt` | Audio effects |
| 8 | AISettingsScreen | `AISettingsScreen.kt` | AI configuration |

### Battle Screens
| # | Screen | File | Purpose |
|---|--------|------|---------|
| 9 | BattleHQScreen | `BattleHQScreen.kt` | Command center |
| 10 | AudioBattleScreen | `AudioBattleScreen.kt` | Battle EQ controls |
| 11 | ActiveBattleScreen | `ActiveBattleScreen.kt` | Auto-battle mode |
| 12 | CounterSongScreen | `CounterSongScreen.kt` | AI counter picks |
| 13 | BattleLibraryScreen | `BattleLibraryScreen.kt` | Battle songs |
| 14 | BattleAnalyzerScreen | `BattleAnalyzerScreen.kt` | Opponent analysis |
| 15 | BattleArmoryScreen | `BattleArmoryScreen.kt` | Pre-saved clips |

---

## UI Components (7 Total)

| Component | File | Purpose |
|-----------|------|---------|
| NowPlayingBar | `NowPlayingBar.kt` | Mini player bottom bar |
| SongListItem | `SongListItem.kt` | Song row in lists |
| SpeedPitchControl | `SpeedPitchControl.kt` | Speed/pitch sliders |
| PresetPanel | `PresetPanel.kt` | Audio presets |
| AlbumArtView | `AlbumArtView.kt` | Album artwork display |
| WaveformView | `WaveformView.kt` | Waveform visualization |
| QuickAddWidget | `QuickAddWidget.kt` | Quick song add |

---

## Audio Engine Architecture

### Playback Chain
```
Song File
    │
    ├─► ExoPlayer (Decoding)
    │       │
    │       ├─► SoundTouch (Speed/Pitch)
    │       │       │
    │       │       └─► NativeBattleEngine
    │       │               │
    │       │               ├─► EQ Processing
    │       │               ├─► Bass Boost
    │       │               ├─► Loudness
    │       │               └─► Battle Effects
    │       │
    │       └─► Audio Output
    │
    └─► MusicController (Control Interface)
            │
            ├─► Play/Pause/Stop
            ├─► Seek
            ├─► Speed/Pitch
            ├─► A-B Loop
            └─► EQ Settings
```

### Battle Audio Features
- **Real-time Opponent Analysis** - Microphone captures opponent's sound
- **Frequency Gap Detection** - Find weak spots in opponent's mix
- **Auto-Counter EQ** - Automatically boost YOUR frequencies where opponent is weak
- **SPL Monitoring** - Track volume levels
- **Venue Profiling** - Adapt to room acoustics

---

## Feature Matrix

### Music Player Features
| Feature | Status | Notes |
|---------|--------|-------|
| Play/Pause/Stop | ✅ | Working |
| Next/Previous | ✅ | Working |
| Shuffle | ✅ | Working |
| Repeat (All/One) | ✅ | Working |
| A-B Loop | ✅ | With visual markers |
| Speed Control | ✅ | 0.05x - 10x |
| Pitch Control | ✅ | -36 to +36 semitones |
| 5-Band EQ | ✅ | 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz |
| Bass Boost | ✅ | 0-1000 |
| Loudness Enhancer | ✅ | 0-1000 |
| Waveform View | ✅ | Interactive seeking |
| Album Art | ✅ | Extracted from files |

### Smart Features
| Feature | Status | Notes |
|---------|--------|-------|
| Auto-Scan Library | ✅ | On first launch |
| Smart Search | ✅ | Fuzzy matching |
| Voice Search | ✅ | Speech recognition |
| Smart Playlists | ✅ | AI-generated |
| Song Metadata | ✅ | BPM, Key detection |

### Battle Features
| Feature | Status | Notes |
|---------|--------|-------|
| Opponent Analysis | ✅ | Real-time microphone |
| Counter EQ Suggestions | ✅ | AI-powered |
| Battle Modes | ✅ | 5 modes |
| Auto-Counter | ✅ | Automatic adjustment |
| Auto-Volume Match | ✅ | Match opponent SPL |
| Auto-Queue | ✅ | Smart song selection |
| Battle Clips | ✅ | Pre-saved moments |
| Venue Profiler | ✅ | Room analysis |

---

## Known Issues & Fixes Needed

### Critical (Must Fix)
| # | Issue | Location | Fix |
|---|-------|----------|-----|
| 1 | `Icons.Default.Stop` doesn't exist | ActiveBattleScreen | Replace with `Icons.Filled.Stop` or custom |
| 2 | Play/Pause icon mismatch | Various screens | Audit all playback icons |
| 3 | Navigation not resetting tabs | HomeScreen | Clear state on navigate |

### High Priority
| # | Issue | Location | Fix |
|---|-------|----------|-----|
| 4 | Theme inconsistency | Battle screens vs Main | Unify dark/light theme |
| 5 | Speed/Pitch range inconsistent | Multiple panels | Standardize values |
| 6 | Missing back navigation | Some screens | Add proper back handlers |
| 7 | A-B Loop panel too complex | NowPlayingScreen | Simplify UX |

### Medium Priority
| # | Issue | Location | Fix |
|---|-------|----------|-----|
| 8 | No loading indicator for waveform | WaveformView | Add shimmer |
| 9 | Search limited to 5 results | QuickSearchBar | Increase limit |
| 10 | Emoji inconsistency | Battle screens | Use icons instead |
| 11 | Accessibility missing | All icons | Add contentDescription |

---

## Color Scheme

### Main App (Light Theme)
```kotlin
Primary = Purple (Material 3 default)
Background = White/Light Gray
Surface = White
OnSurface = Dark Gray/Black
```

### Battle Mode (Dark Theme)
```kotlin
Background = #1A1A2E (Deep Navy)
Surface = #16213E (Dark Blue)
Primary = #FF6B35 (Battle Orange)
Accent = #4ECDC4 (Cyber Teal)
```

### Speed/Pitch Indicators
```kotlin
SpeedFast = #FF5722 (Orange)
SpeedSlow = #2196F3 (Blue)
PitchHigh = #E91E63 (Pink)
PitchLow = #9C27B0 (Purple)
```

---

## File Structure

```
app/src/main/
├── java/com/ultramusic/player/
│   ├── MainActivity.kt          # Entry point
│   ├── UltraMusicApp.kt         # Application class
│   │
│   ├── ui/
│   │   ├── screens/             # 15 screen composables
│   │   ├── components/          # 7 UI components
│   │   ├── theme/               # Theme definitions
│   │   ├── MainViewModel.kt     # Central state
│   │   └── MainUIState.kt       # State classes
│   │
│   ├── data/
│   │   ├── Models.kt            # Data classes
│   │   ├── MusicRepository.kt   # Data access
│   │   ├── FolderRepository.kt  # File system
│   │   └── SmartPlaylistManager.kt
│   │
│   ├── audio/
│   │   ├── MusicPlaybackService.kt
│   │   ├── MusicController.kt
│   │   ├── AudioBattleEngine.kt
│   │   ├── BattleIntelligence.kt
│   │   └── NativeBattleEngine.kt
│   │
│   ├── core/
│   │   ├── BattleArmory.kt
│   │   ├── BattleSongDatabase.kt
│   │   └── VenueProfiler.kt
│   │
│   ├── ai/
│   │   ├── GrokAIService.kt
│   │   └── CounterSongEngine.kt
│   │
│   └── di/
│       └── AppModule.kt         # Hilt DI
│
├── cpp/
│   └── battle_audio_engine.cpp  # Native audio
│
└── res/
    ├── drawable/                # Icons, images
    ├── values/                  # Strings, colors
    └── xml/                     # Config files
```

---

## Premium Quality Checklist

### UI/UX Excellence
- [ ] Consistent iconography throughout
- [ ] Smooth animations (300ms standard)
- [ ] Haptic feedback on buttons
- [ ] Pull-to-refresh on lists
- [ ] Swipe gestures support
- [ ] Loading states for all async operations
- [ ] Error states with retry options
- [ ] Empty states with helpful messages

### Performance
- [ ] Lazy loading for long lists
- [ ] Image caching for album art
- [ ] Background service for playback
- [ ] Efficient memory usage
- [ ] Fast app startup (<2s)

### Accessibility
- [ ] Content descriptions on all icons
- [ ] TalkBack support
- [ ] Minimum touch target 48dp
- [ ] Color contrast ratio 4.5:1

### Polish
- [ ] Splash screen with branding
- [ ] App icon (adaptive)
- [ ] Themed system bars
- [ ] Rounded corners consistency
- [ ] Shadow/elevation consistency
- [ ] Typography scale adherence

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | Initial | Core player |
| 1.1.0 | + Battle | Battle features |
| 1.2.0 | + AI | Grok AI integration |
| 1.3.0 | + Auto-Scan | Permission flow |
| 1.4.0 | (Current) | Premium polish |

---

## Next Steps (Priority Order)

1. **Fix Critical Icons** - Replace missing Stop icon
2. **Audit All Play/Pause** - Ensure icon matches state
3. **Unify Theme** - Consistent dark/light
4. **Navigation Polish** - Proper back handling
5. **Loading States** - Add to all async operations
6. **Accessibility** - Add content descriptions
7. **Performance** - Optimize lists and images
8. **Testing** - Manual testing of all flows

---

*This document serves as the single source of truth for UltraMusic Player development.*
