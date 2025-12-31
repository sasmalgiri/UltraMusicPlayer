# UltraMusic Player - Pre-Release Testing Checklist

## Version: 1.0.0 PRODUCTION
## Date: December 2024

---

## 🔴 CRITICAL TESTS (Must Pass Before Release)

### 1. App Startup
- [ ] App launches without crash
- [ ] Splash screen displays correctly
- [ ] Main screen loads within 3 seconds
- [ ] No ANR (Application Not Responding) dialogs

### 2. Permission Handling
- [ ] Storage permission request shows on first launch
- [ ] App works after granting storage permission
- [ ] App shows appropriate message if permission denied
- [ ] Microphone permission request shows when needed (voice search, battle)
- [ ] App gracefully handles denied microphone permission

### 3. Music Library
- [ ] Songs scan from device storage
- [ ] Song count matches device music files
- [ ] Album art loads correctly
- [ ] Song metadata (title, artist, album) displays correctly
- [ ] Empty state shows if no music found

### 4. Basic Playback
- [ ] Tap song starts playback
- [ ] Play/Pause button works
- [ ] Next/Previous track works
- [ ] Seek bar allows position change
- [ ] Song progress updates correctly
- [ ] Audio outputs through speaker
- [ ] Audio outputs through headphones
- [ ] Audio outputs through Bluetooth

---

## 🟡 IMPORTANT TESTS (Should Pass)

### 5. Speed & Pitch Control
- [ ] Speed slider changes playback speed
- [ ] Minimum speed (0.05x) works
- [ ] Maximum speed (10.0x) works
- [ ] Pitch slider changes pitch
- [ ] Minimum pitch (-36 semitones) works
- [ ] Maximum pitch (+36 semitones) works
- [ ] Reset button returns to normal
- [ ] Presets (Nightcore, Slowed, etc.) apply correctly

### 6. Search & Browse
- [ ] Search finds songs by title
- [ ] Search finds songs by artist
- [ ] Search results update as you type
- [ ] Clear search works
- [ ] Folder browser shows folder structure
- [ ] Navigating into folders works
- [ ] Back button in folders works

### 7. Playlist Features
- [ ] Add song to playlist works
- [ ] Remove song from playlist works
- [ ] Playlist saves between sessions
- [ ] Shuffle works correctly
- [ ] Repeat modes work (off, one, all)

### 8. Background Playback
- [ ] Music continues when app minimized
- [ ] Notification controls appear
- [ ] Play/Pause from notification works
- [ ] Next/Previous from notification works
- [ ] Music stops when notification dismissed

### 9. Audio Quality
- [ ] No audio crackling or distortion
- [ ] No audio dropouts during playback
- [ ] Smooth transitions between songs
- [ ] Volume control responsive

---

## 🟢 BATTLE FEATURES (Unique Selling Points)

### 10. Battle HQ (Passive Controls)
- [ ] EQ bands respond to adjustment
- [ ] Bass boost slider works
- [ ] Loudness enhancer works
- [ ] Virtualizer/spatial audio works
- [ ] Battle presets apply correctly
- [ ] Settings persist during playback

### 11. Active Battle Screen
- [ ] Battle starts without crash
- [ ] Momentum display updates
- [ ] Auto-counter toggle works
- [ ] Auto-volume toggle works
- [ ] Battle mode selector works
- [ ] Battle log updates with events
- [ ] Battle ends cleanly

### 12. Opponent Detection (Requires Microphone)
- [ ] Real-time audio analysis activates
- [ ] Loudness meter responds to external sound
- [ ] Energy detection works
- [ ] No crash when microphone unavailable

### 13. AI Features
- [ ] Counter song recommendations appear
- [ ] Song suggestions make sense (BPM/Key matching)
- [ ] Battle library categorization works

---

## 📱 DEVICE COMPATIBILITY

### Test on Multiple Devices:
- [ ] Android 8.0 (API 26)
- [ ] Android 10.0 (API 29)
- [ ] Android 12.0 (API 31)
- [ ] Android 13.0 (API 33)
- [ ] Android 14.0 (API 34)

### Screen Sizes:
- [ ] Phone (small screen)
- [ ] Phone (large screen)
- [ ] Tablet (if applicable)

### Manufacturers:
- [ ] Samsung
- [ ] Xiaomi
- [ ] OnePlus
- [ ] Google Pixel
- [ ] Other

---

## ⚡ PERFORMANCE TESTS

### Memory
- [ ] RAM usage stays under 200MB during playback
- [ ] No memory leaks (check after 30 min use)
- [ ] App doesn't crash when switching between screens

### Battery
- [ ] Battery drain is reasonable during playback
- [ ] Background playback doesn't drain excessively

### Responsiveness
- [ ] UI responds within 100ms to taps
- [ ] Scrolling is smooth (60 FPS)
- [ ] No jank when loading large libraries

---

## 🔧 EDGE CASES

### Error Handling
- [ ] App handles corrupted audio files gracefully
- [ ] App handles missing files gracefully
- [ ] Network errors don't crash app
- [ ] Permission revocation handled gracefully

### Interruptions
- [ ] Incoming call pauses music
- [ ] Music resumes after call ends
- [ ] Other app audio focus handled correctly
- [ ] Headphone disconnect pauses music

### Storage
- [ ] Works with internal storage
- [ ] Works with SD card storage
- [ ] Works with large libraries (1000+ songs)

---

## 📝 PRE-RELEASE CHECKLIST

### Code Quality
- [ ] No compiler warnings
- [ ] ProGuard/R8 configured correctly
- [ ] Debug code removed
- [ ] API keys secured (not in code)

### Store Requirements
- [ ] App icon ready (all sizes)
- [ ] Feature graphic ready
- [ ] Screenshots prepared
- [ ] Privacy policy URL ready
- [ ] App description written

### Legal
- [ ] All third-party licenses documented
- [ ] No copyrighted assets used
- [ ] Media3/ExoPlayer license compliant

---

## 🐛 BUG TRACKING

| Bug ID | Description | Severity | Status |
|--------|-------------|----------|--------|
| BUG-001 | | | |
| BUG-002 | | | |
| BUG-003 | | | |

---

## 📊 TEST RESULTS SUMMARY

**Tester Name:** ________________
**Device:** ________________
**Android Version:** ________________
**Test Date:** ________________

### Results:
- Critical Tests: ___ / 12 passed
- Important Tests: ___ / 27 passed  
- Battle Features: ___ / 13 passed
- Performance Tests: ___ / 8 passed

### Overall Status: 
- [ ] ✅ READY FOR RELEASE
- [ ] ⚠️ NEEDS FIXES (see bugs)
- [ ] ❌ CRITICAL ISSUES

### Notes:
```
Add any additional observations here...
```

---

## 🚀 POST-TEST ACTIONS

1. **Fix all Critical bugs** before release
2. **Document known issues** for users
3. **Prepare hotfix plan** for post-launch issues
4. **Set up crash reporting** (Firebase Crashlytics recommended)
5. **Monitor reviews** after launch

---

*Generated by UltraMusic Player Development Team*
