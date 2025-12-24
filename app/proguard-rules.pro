# UltraMusic Player ProGuard Rules
# =================================

# ==================== NATIVE/JNI ====================

# Keep native methods and JNI bridge
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep NativeBattleEngine - critical for audio processing
-keep class com.ultramusic.player.audio.NativeBattleEngine { *; }
-keep class com.ultramusic.player.audio.NativeBattleEngine$Companion { *; }

# Keep audio processor interfaces and callbacks
-keep class com.ultramusic.player.audio.BattleAudioProcessor { *; }
-keep interface com.ultramusic.player.audio.** { *; }

# ==================== HILT/DI ====================

-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Hilt-injected classes
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @javax.inject.Singleton class * { *; }

# ==================== MEDIA3/EXOPLAYER ====================

-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep audio effect processors
-keep class com.ultramusic.player.audio.** { *; }

# ==================== COMPOSE ====================

-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ==================== DATA CLASSES ====================

-keep class com.ultramusic.player.data.** { *; }

# Keep Song class for JSON serialization
-keep class com.ultramusic.player.data.Song { *; }
-keepclassmembers class com.ultramusic.player.data.Song { *; }

# ==================== ENUMS ====================

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep audio enums
-keep enum com.ultramusic.player.audio.BattleMode { *; }
-keep enum com.ultramusic.player.audio.QualityMode { *; }

# ==================== SERIALIZATION ====================

# Keep Gson annotations
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# ==================== COROUTINES ====================

-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ==================== OKHTTP ====================

-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ==================== DEBUG REMOVAL ====================

# Remove debug/verbose logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

# ==================== OPTIMIZATION ====================

# Don't warn about missing classes in optional dependencies
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
