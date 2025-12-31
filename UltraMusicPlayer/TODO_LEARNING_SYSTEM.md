# TODO: Battle Learning System (Future Implementation)

## Overview
Implement a Supabase-powered learning system that improves battle performance with each battle.

---

## Current State (What Exists)

### Local Analysis (Working)
- Real-time opponent frequency analysis via microphone
- BPM detection using FFT + auto-correlation
- Song indexing with battle ratings (stored in SharedPreferences)
- Momentum tracking during battles

### What's Missing
- No battle history persistence
- No win/loss tracking
- No cross-session learning
- No collective intelligence from other users

---

## Proposed Architecture

### 1. Supabase Tables

```sql
-- User battles history
CREATE TABLE battles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users,
    started_at TIMESTAMP DEFAULT NOW(),
    ended_at TIMESTAMP,
    result TEXT, -- 'WIN', 'LOSS', 'DRAW'
    final_momentum INT, -- 0-100
    opponent_strategy TEXT, -- 'BASS_HEAVY', 'CLARITY_FOCUSED', etc.
    our_strategy TEXT,
    venue_type TEXT -- 'CLUB', 'OUTDOOR', 'SMALL_ROOM'
);

-- Songs used in battles with outcomes
CREATE TABLE battle_songs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    battle_id UUID REFERENCES battles,
    song_id BIGINT, -- Local MediaStore ID
    song_title TEXT,
    song_artist TEXT,
    played_at TIMESTAMP,
    momentum_before INT,
    momentum_after INT,
    opponent_strategy_at_time TEXT,
    effectiveness_score INT -- Calculated from momentum change
);

-- Aggregated song effectiveness (collective learning)
CREATE TABLE song_effectiveness (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    song_hash TEXT UNIQUE, -- Hash of title+artist for matching
    song_title TEXT,
    song_artist TEXT,
    total_plays INT DEFAULT 0,
    wins INT DEFAULT 0,
    vs_bass_heavy_wins INT DEFAULT 0,
    vs_bass_heavy_plays INT DEFAULT 0,
    vs_clarity_focused_wins INT DEFAULT 0,
    vs_clarity_focused_plays INT DEFAULT 0,
    vs_vocal_heavy_wins INT DEFAULT 0,
    vs_vocal_heavy_plays INT DEFAULT 0,
    avg_momentum_gain FLOAT DEFAULT 0,
    last_updated TIMESTAMP DEFAULT NOW()
);

-- EQ settings that worked
CREATE TABLE effective_eq_settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    opponent_strategy TEXT,
    eq_band_0 INT, -- 60Hz
    eq_band_1 INT, -- 230Hz
    eq_band_2 INT, -- 910Hz
    eq_band_3 INT, -- 3.6kHz
    eq_band_4 INT, -- 14kHz
    bass_boost INT,
    loudness_boost INT,
    success_count INT DEFAULT 0,
    total_uses INT DEFAULT 0
);
```

### 2. Learning Flow

```
BATTLE START
    ↓
Record: opponent_strategy, venue_type, start_time
    ↓
DURING BATTLE
    ↓
For each song played:
    - Record momentum before/after
    - Record opponent strategy at time
    - Calculate effectiveness
    ↓
BATTLE END
    ↓
Upload to Supabase:
    - Battle record
    - All songs with effectiveness
    - Final result
    ↓
UPDATE LOCAL MODEL
    ↓
Pull aggregated data:
    - Best songs vs each strategy
    - Optimal EQ settings
    - Win rate statistics
```

### 3. Kotlin Implementation Outline

```kotlin
// BattleLearningRepository.kt
@Singleton
class BattleLearningRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val battleSongDB: BattleSongDatabase
) {
    // Start tracking a new battle
    suspend fun startBattle(opponentStrategy: OpponentStrategy): String

    // Record song played during battle
    suspend fun recordSongPlayed(
        battleId: String,
        song: Song,
        momentumBefore: Int,
        momentumAfter: Int,
        opponentStrategy: OpponentStrategy
    )

    // End battle and upload results
    suspend fun endBattle(
        battleId: String,
        result: BattleResult,
        finalMomentum: Int
    )

    // Get best counter songs from collective data
    suspend fun getBestCounterSongs(
        opponentStrategy: OpponentStrategy,
        limit: Int = 10
    ): List<RecommendedSong>

    // Get optimal EQ for opponent type
    suspend fun getOptimalEQ(
        opponentStrategy: OpponentStrategy
    ): EQSettings

    // Sync local song ratings with cloud data
    suspend fun syncSongRatings()
}
```

### 4. Privacy Considerations

- **Anonymous by default** - No user identification required
- **Song hashes only** - Don't upload full file paths
- **Opt-in sync** - User must enable cloud learning
- **Local-first** - App works fully offline
- **Data deletion** - Users can delete their battle history

### 5. Supabase Setup Steps

1. Create Supabase project at supabase.com
2. Run SQL migrations for tables above
3. Enable Row Level Security (RLS)
4. Create API key for Android app
5. Add Supabase Kotlin SDK to build.gradle:
   ```gradle
   implementation("io.github.jan-tennert.supabase:postgrest-kt:2.0.0")
   implementation("io.github.jan-tennert.supabase:realtime-kt:2.0.0")
   ```

### 6. Features Enabled by Learning

| Feature | Description |
|---------|-------------|
| **Smart Counter Picks** | "This song wins 85% against bass-heavy opponents" |
| **Personalized EQ** | "Your best settings vs clarity-focused: +6dB bass, +3dB highs" |
| **Win Rate Tracking** | "You've won 23 of 31 battles (74%)" |
| **Weakness Analysis** | "You struggle against vocal-heavy opponents, try these songs..." |
| **Global Leaderboard** | Optional competitive rankings |
| **Battle Replay** | Review past battles with timeline |

---

## Implementation Priority

### Phase 1: Local History (No Supabase)
- [ ] Save battle results to local Room database
- [ ] Track win/loss per song locally
- [ ] Show personal statistics screen

### Phase 2: Cloud Sync (Supabase)
- [ ] Setup Supabase project
- [ ] Implement upload on battle end
- [ ] Pull collective song ratings

### Phase 3: Smart Recommendations
- [ ] Use cloud data for counter picks
- [ ] Suggest EQ based on success rates
- [ ] Identify personal weak points

### Phase 4: Advanced Features
- [ ] Real-time battle sharing
- [ ] Leaderboards
- [ ] Battle replays

---

## Estimated Effort

| Phase | Time Estimate |
|-------|---------------|
| Phase 1 | 2-3 days |
| Phase 2 | 3-4 days |
| Phase 3 | 2-3 days |
| Phase 4 | 4-5 days |

**Total: ~2 weeks for full implementation**

---

## Notes

- Grok AI can be used alongside learning for enhanced recommendations
- Learning system should have offline fallback
- Consider rate limiting to prevent abuse
- Add analytics for popular songs/strategies
