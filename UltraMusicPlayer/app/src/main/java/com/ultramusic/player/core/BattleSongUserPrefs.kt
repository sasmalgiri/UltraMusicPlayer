package com.ultramusic.player.core

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BattleSongUserPrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private const val PREFS_NAME = "battle_song_user_prefs"
        private const val KEY_FAVORITES = "favorite_song_ids"
        private const val KEY_RECENT = "recent_battle_song_ids"
        private const val MAX_RECENT = 50
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _favoriteSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteSongIds: StateFlow<Set<Long>> = _favoriteSongIds.asStateFlow()

    private val _recentBattleSongIds = MutableStateFlow<List<Long>>(emptyList())
    val recentBattleSongIds: StateFlow<List<Long>> = _recentBattleSongIds.asStateFlow()

    init {
        _favoriteSongIds.value = loadLongSet(KEY_FAVORITES)
        _recentBattleSongIds.value = loadLongList(KEY_RECENT)
    }

    fun isFavorite(songId: Long): Boolean = _favoriteSongIds.value.contains(songId)

    fun toggleFavorite(songId: Long) {
        val current = _favoriteSongIds.value
        val updated = if (current.contains(songId)) current - songId else current + songId
        _favoriteSongIds.value = updated
        saveLongSet(KEY_FAVORITES, updated)
    }

    fun recordRecent(songId: Long) {
        val current = _recentBattleSongIds.value
        val updated = buildList {
            add(songId)
            current.forEach { existing ->
                if (existing != songId) add(existing)
            }
        }.take(MAX_RECENT)

        _recentBattleSongIds.value = updated
        saveLongList(KEY_RECENT, updated)
    }

    private fun saveLongSet(key: String, values: Set<Long>) {
        val array = JSONArray()
        values.forEach { array.put(it) }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun loadLongSet(key: String): Set<Long> {
        val raw = prefs.getString(key, null) ?: return emptySet()
        return try {
            val array = JSONArray(raw)
            buildSet {
                for (i in 0 until array.length()) {
                    add(array.optLong(i))
                }
            }
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun saveLongList(key: String, values: List<Long>) {
        val array = JSONArray()
        values.forEach { array.put(it) }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun loadLongList(key: String): List<Long> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    add(array.optLong(i))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
