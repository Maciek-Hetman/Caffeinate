package com.maciejhetman.caffeinate.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "caffeinate_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class CaffeinePreferences(private val context: Context) {

    val settings: Flow<CaffeineSettings> = context.dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            CaffeineSettings(
                lastDuration = DurationPreset.fromSerialized(prefs[KEY_LAST_DURATION]),
                widgetTimerDuration = prefs[KEY_WIDGET_TIMER_DURATION].toTimedOrDefault(),
                themeMode = ThemeMode.fromSerialized(prefs[KEY_THEME_MODE]),
                dynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: true,
                stopOnScreenOff = prefs[KEY_STOP_ON_SCREEN_OFF] ?: false,
            )
        }

    suspend fun setLastDuration(duration: DurationPreset) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_DURATION] = duration.serialize()
        }
    }

    suspend fun setWidgetTimerDuration(duration: DurationPreset.Timed) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WIDGET_TIMER_DURATION] = duration.serialize()
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.serialize()
        }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setStopOnScreenOff(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_STOP_ON_SCREEN_OFF] = enabled
        }
    }

    companion object {
        private val KEY_LAST_DURATION = stringPreferencesKey("last_duration")
        private val KEY_WIDGET_TIMER_DURATION = stringPreferencesKey("widget_timer_duration")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled")
        private val KEY_STOP_ON_SCREEN_OFF = booleanPreferencesKey("stop_on_screen_off")

        private fun String?.toTimedOrDefault(): DurationPreset.Timed {
            if (this == null) return DurationPreset.DefaultTimed
            return when (val parsed = DurationPreset.fromSerialized(this)) {
                is DurationPreset.Timed -> parsed
                DurationPreset.Infinite -> DurationPreset.DefaultTimed
            }
        }
    }
}
