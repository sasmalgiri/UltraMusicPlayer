package com.ultramusic.player.di

import android.content.Context
import com.ultramusic.player.ai.CounterSongEngine
import com.ultramusic.player.ai.GrokAIService
import com.ultramusic.player.audio.ActiveBattleSystem
import com.ultramusic.player.audio.AudioBattleEngine
import com.ultramusic.player.audio.AudioQualityManager
import com.ultramusic.player.audio.BattleIntelligence
import com.ultramusic.player.audio.CrowdAnalyzer
import com.ultramusic.player.audio.FrequencyWarfare
import com.ultramusic.player.audio.NativeBattleEngine
import com.ultramusic.player.audio.SongBattleAnalyzer
import com.ultramusic.player.audio.VenueProfiler
import com.ultramusic.player.audio.ExtremeNoiseVoiceCapture
import com.ultramusic.player.audio.MusicController
import com.ultramusic.player.audio.VoiceSearchManager
import com.ultramusic.player.core.AppErrorHandler
import com.ultramusic.player.core.AutoClipDetector
import com.ultramusic.player.core.BattleArmory
import com.ultramusic.player.core.BattleClipStore
import com.ultramusic.player.core.BattleSongDatabase
import com.ultramusic.player.core.ProductionBattleAI
import com.ultramusic.player.core.RealAudioAnalyzer
import com.ultramusic.player.core.LocalBattleAnalyzer
import com.ultramusic.player.data.FolderRepository
import com.ultramusic.player.data.MusicRepository
import com.ultramusic.player.data.SmartPlaylistManager
import com.ultramusic.player.data.SmartSearchEngine
import com.ultramusic.player.data.SongMetadataManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    // ==================== CORE PRODUCTION SYSTEMS ====================
    
    @Provides
    @Singleton
    fun provideAppErrorHandler(
        @ApplicationContext context: Context
    ): AppErrorHandler {
        return AppErrorHandler(context)
    }
    
    @Provides
    @Singleton
    fun provideRealAudioAnalyzer(
        @ApplicationContext context: Context
    ): RealAudioAnalyzer {
        return RealAudioAnalyzer(context)
    }
    
    @Provides
    @Singleton
    fun provideBattleSongDatabase(
        @ApplicationContext context: Context
    ): BattleSongDatabase {
        return BattleSongDatabase(context)
    }
    
    @Provides
    @Singleton
    fun provideBattleArmory(
        @ApplicationContext context: Context
    ): BattleArmory {
        return BattleArmory(context)
    }
    
    @Provides
    @Singleton
    fun provideAutoClipDetector(
        @ApplicationContext context: Context
    ): AutoClipDetector {
        return AutoClipDetector(context)
    }
    
    @Provides
    @Singleton
    fun provideBattleClipStore(
        @ApplicationContext context: Context
    ): BattleClipStore {
        return BattleClipStore(context)
    }
    
    @Provides
    @Singleton
    fun provideGrokAIService(
        @ApplicationContext context: Context
    ): GrokAIService {
        return GrokAIService(context)
    }
    
    @Provides
    @Singleton
    fun provideLocalBattleAnalyzer(
        @ApplicationContext context: Context,
        battleSongDatabase: BattleSongDatabase
    ): LocalBattleAnalyzer {
        return LocalBattleAnalyzer(context, battleSongDatabase)
    }
    
    @Provides
    @Singleton
    fun provideProductionBattleAI(
        @ApplicationContext context: Context,
        realAudioAnalyzer: RealAudioAnalyzer,
        appErrorHandler: AppErrorHandler
    ): ProductionBattleAI {
        return ProductionBattleAI(context, realAudioAnalyzer, appErrorHandler)
    }
    
    // ==================== DATA REPOSITORIES ====================
    
    @Provides
    @Singleton
    fun provideMusicRepository(
        @ApplicationContext context: Context
    ): MusicRepository {
        return MusicRepository(context)
    }
    
    @Provides
    @Singleton
    fun provideFolderRepository(
        @ApplicationContext context: Context
    ): FolderRepository {
        return FolderRepository(context)
    }
    
    // ==================== AUDIO PLAYBACK ====================
    
    @Provides
    @Singleton
    fun provideMusicController(
        @ApplicationContext context: Context,
        nativeBattleEngine: NativeBattleEngine
    ): MusicController {
        return MusicController(context, nativeBattleEngine)
    }
    
    @Provides
    @Singleton
    fun provideVoiceSearchManager(
        @ApplicationContext context: Context
    ): VoiceSearchManager {
        return VoiceSearchManager(context)
    }
    
    @Provides
    @Singleton
    fun provideExtremeNoiseVoiceCapture(
        @ApplicationContext context: Context
    ): ExtremeNoiseVoiceCapture {
        return ExtremeNoiseVoiceCapture(context)
    }
    
    @Provides
    @Singleton
    fun provideAudioQualityManager(
        @ApplicationContext context: Context
    ): AudioQualityManager {
        return AudioQualityManager(context)
    }
    
    // ==================== SEARCH & PLAYLIST ====================
    
    @Provides
    @Singleton
    fun provideSmartSearchEngine(): SmartSearchEngine {
        return SmartSearchEngine()
    }
    
    @Provides
    @Singleton
    fun provideSongMetadataManager(
        @ApplicationContext context: Context
    ): SongMetadataManager {
        return SongMetadataManager(context)
    }
    
    @Provides
    @Singleton
    fun provideSmartPlaylistManager(
        smartSearchEngine: SmartSearchEngine
    ): SmartPlaylistManager {
        return SmartPlaylistManager(smartSearchEngine)
    }
    
    // ==================== AI SYSTEMS ====================
    
    @Provides
    @Singleton
    fun provideCounterSongEngine(
        @ApplicationContext context: Context
    ): CounterSongEngine {
        return CounterSongEngine(context)
    }
    
    // ==================== BATTLE SYSTEMS ====================
    
    @Provides
    @Singleton
    fun provideAudioBattleEngine(
        @ApplicationContext context: Context
    ): AudioBattleEngine {
        return AudioBattleEngine(context)
    }
    
    @Provides
    @Singleton
    fun provideBattleIntelligence(
        @ApplicationContext context: Context
    ): BattleIntelligence {
        return BattleIntelligence(context)
    }
    
    @Provides
    @Singleton
    fun provideSongBattleAnalyzer(
        @ApplicationContext context: Context
    ): SongBattleAnalyzer {
        return SongBattleAnalyzer(context)
    }
    
    @Provides
    @Singleton
    fun provideVenueProfiler(
        @ApplicationContext context: Context
    ): VenueProfiler {
        return VenueProfiler(context)
    }
    
    @Provides
    @Singleton
    fun provideActiveBattleSystem(
        @ApplicationContext context: Context
    ): ActiveBattleSystem {
        return ActiveBattleSystem(context)
    }
    
    @Provides
    @Singleton
    fun provideCrowdAnalyzer(
        @ApplicationContext context: Context
    ): CrowdAnalyzer {
        return CrowdAnalyzer(context)
    }
    
    @Provides
    @Singleton
    fun provideFrequencyWarfare(): FrequencyWarfare {
        return FrequencyWarfare()
    }
    
    @Provides
    @Singleton
    fun provideNativeBattleEngine(): NativeBattleEngine {
        return NativeBattleEngine()
    }
}
