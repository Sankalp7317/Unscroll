package com.example.unscroll.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val YOUTUBE_LIMIT_MS = longPreferencesKey("youtube_limit_ms")
        val INSTAGRAM_LIMIT_MS = longPreferencesKey("instagram_limit_ms")
        val TIKTOK_LIMIT_MS = longPreferencesKey("tiktok_limit_ms")

        val YOUTUBE_USAGE_MS = longPreferencesKey("youtube_usage_ms")
        val INSTAGRAM_USAGE_MS = longPreferencesKey("instagram_usage_ms")
        val TIKTOK_USAGE_MS = longPreferencesKey("tiktok_usage_ms")

        val YOUTUBE_ENABLED = booleanPreferencesKey("youtube_enabled")
        val INSTAGRAM_ENABLED = booleanPreferencesKey("instagram_enabled")
        val TIKTOK_ENABLED = booleanPreferencesKey("tiktok_enabled")

        val LAST_RESET_TIMESTAMP = longPreferencesKey("last_reset_timestamp")
        
        const val DEFAULT_LIMIT_MS = 30 * 60 * 1000L // 30 minutes
    }

    val youtubeLimit: Flow<Long> = context.dataStore.data.map { it[YOUTUBE_LIMIT_MS] ?: DEFAULT_LIMIT_MS }
    val instagramLimit: Flow<Long> = context.dataStore.data.map { it[INSTAGRAM_LIMIT_MS] ?: DEFAULT_LIMIT_MS }
    val tiktokLimit: Flow<Long> = context.dataStore.data.map { it[TIKTOK_LIMIT_MS] ?: DEFAULT_LIMIT_MS }

    val youtubeUsage: Flow<Long> = context.dataStore.data.map { it[YOUTUBE_USAGE_MS] ?: 0L }
    val instagramUsage: Flow<Long> = context.dataStore.data.map { it[INSTAGRAM_USAGE_MS] ?: 0L }
    val tiktokUsage: Flow<Long> = context.dataStore.data.map { it[TIKTOK_USAGE_MS] ?: 0L }

    val youtubeEnabled: Flow<Boolean> = context.dataStore.data.map { it[YOUTUBE_ENABLED] ?: true }
    val instagramEnabled: Flow<Boolean> = context.dataStore.data.map { it[INSTAGRAM_ENABLED] ?: true }
    val tiktokEnabled: Flow<Boolean> = context.dataStore.data.map { it[TIKTOK_ENABLED] ?: true }

    val lastResetTimestamp: Flow<Long> = context.dataStore.data.map { it[LAST_RESET_TIMESTAMP] ?: 0L }

    suspend fun updateYoutubeLimit(limitMs: Long) = context.dataStore.edit { it[YOUTUBE_LIMIT_MS] = limitMs }
    suspend fun updateInstagramLimit(limitMs: Long) = context.dataStore.edit { it[INSTAGRAM_LIMIT_MS] = limitMs }
    suspend fun updateTiktokLimit(limitMs: Long) = context.dataStore.edit { it[TIKTOK_LIMIT_MS] = limitMs }

    suspend fun updateYoutubeEnabled(enabled: Boolean) = context.dataStore.edit { it[YOUTUBE_ENABLED] = enabled }
    suspend fun updateInstagramEnabled(enabled: Boolean) = context.dataStore.edit { it[INSTAGRAM_ENABLED] = enabled }
    suspend fun updateTiktokEnabled(enabled: Boolean) = context.dataStore.edit { it[TIKTOK_ENABLED] = enabled }

    suspend fun updateYoutubeUsage(usageMs: Long) = context.dataStore.edit { it[YOUTUBE_USAGE_MS] = usageMs }
    suspend fun updateInstagramUsage(usageMs: Long) = context.dataStore.edit { it[INSTAGRAM_USAGE_MS] = usageMs }
    suspend fun updateTiktokUsage(usageMs: Long) = context.dataStore.edit { it[TIKTOK_USAGE_MS] = usageMs }

    suspend fun updateLastResetTimestamp(timestamp: Long) = context.dataStore.edit { it[LAST_RESET_TIMESTAMP] = timestamp }

    suspend fun resetDailyUsage() {
        context.dataStore.edit { preferences ->
            preferences[YOUTUBE_USAGE_MS] = 0L
            preferences[INSTAGRAM_USAGE_MS] = 0L
            preferences[TIKTOK_USAGE_MS] = 0L
            preferences[LAST_RESET_TIMESTAMP] = System.currentTimeMillis()
        }
    }
}
