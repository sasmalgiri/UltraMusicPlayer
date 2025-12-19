package com.ultramusic.player.di

import android.content.Context
import com.ultramusic.player.audio.NativeAudioEngine
import com.ultramusic.player.data.repository.MusicScanner
import com.ultramusic.player.utils.VoiceSearchHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideNativeAudioEngine(): NativeAudioEngine {
        return NativeAudioEngine()
    }
    
    @Provides
    @Singleton
    fun provideMusicScanner(
        @ApplicationContext context: Context
    ): MusicScanner {
        return MusicScanner(context)
    }
    
    @Provides
    @Singleton
    fun provideVoiceSearchHandler(
        @ApplicationContext context: Context
    ): VoiceSearchHandler {
        return VoiceSearchHandler(context)
    }
}
