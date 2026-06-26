package com.myreader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.myreader.MyReaderApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Global DataStore instance */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object AppPreferences {

    private val context: Context get() = MyReaderApp.instance

    // ===== Keys =====
    private val KEY_PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
    private val KEY_SKIP_FORWARD_SEC = intPreferencesKey("skip_forward_sec")
    private val KEY_SKIP_BACKWARD_SEC = intPreferencesKey("skip_backward_sec")
    private val KEY_SLEEP_TIMER_MIN = intPreferencesKey("sleep_timer_min")
    private val KEY_AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")

    // ===== Defaults =====
    const val DEFAULT_SPEED = 1.0f
    const val DEFAULT_SKIP_FORWARD = 30
    const val DEFAULT_SKIP_BACKWARD = 15
    const val DEFAULT_SLEEP_TIMER = 0
    const val DEFAULT_AUTO_PLAY_NEXT = true

    // ===== Flows =====
    val playbackSpeed: Flow<Float> = context.dataStore.data.map { it[KEY_PLAYBACK_SPEED] ?: DEFAULT_SPEED }
    val skipForwardSeconds: Flow<Int> = context.dataStore.data.map { it[KEY_SKIP_FORWARD_SEC] ?: DEFAULT_SKIP_FORWARD }
    val skipBackwardSeconds: Flow<Int> = context.dataStore.data.map { it[KEY_SKIP_BACKWARD_SEC] ?: DEFAULT_SKIP_BACKWARD }
    val sleepTimerMinutes: Flow<Int> = context.dataStore.data.map { it[KEY_SLEEP_TIMER_MIN] ?: DEFAULT_SLEEP_TIMER }
    val autoPlayNext: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_PLAY_NEXT] ?: DEFAULT_AUTO_PLAY_NEXT }

    // ===== Setters =====
    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[KEY_PLAYBACK_SPEED] = speed }
    }

    suspend fun setSkipForwardSeconds(seconds: Int) {
        context.dataStore.edit { it[KEY_SKIP_FORWARD_SEC] = seconds.coerceIn(5, 120) }
    }

    suspend fun setSkipBackwardSeconds(seconds: Int) {
        context.dataStore.edit { it[KEY_SKIP_BACKWARD_SEC] = seconds.coerceIn(5, 120) }
    }

    suspend fun setSleepTimerMinutes(minutes: Int) {
        context.dataStore.edit { it[KEY_SLEEP_TIMER_MIN] = minutes }
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_PLAY_NEXT] = enabled }
    }
}
