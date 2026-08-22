package com.maciejhetman.caffeinate.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "caffeinate_prefs")

class CaffeinePreferences(private val context: Context) {

    val lastDuration: Flow<DurationPreset> = context.dataStore.data.map { prefs ->
        DurationPreset.fromSerialized(prefs[KEY_LAST_DURATION])
    }

    val widgetTimerDuration: Flow<DurationPreset.Timed> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIDGET_TIMER_DURATION].toTimedOrDefault()
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.fromSerialized(prefs[KEY_THEME_MODE])
    }

    val dynamicColorEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_COLOR] ?: true
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

    suspend fun getLastDurationOnce(): DurationPreset = lastDuration.first()

    suspend fun getWidgetTimerDurationOnce(): DurationPreset.Timed = widgetTimerDuration.first()

    companion object {
        private val KEY_LAST_DURATION = stringPreferencesKey("last_duration")
        private val KEY_WIDGET_TIMER_DURATION = stringPreferencesKey("widget_timer_duration")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled")

        private fun String?.toTimedOrDefault(): DurationPreset.Timed {
            if (this == null) return DurationPreset.DefaultTimed
            return when (val parsed = DurationPreset.fromSerialized(this)) {
                is DurationPreset.Timed -> parsed
                DurationPreset.Infinite -> DurationPreset.DefaultTimed
            }
        }
    }
}
