package com.maciejhetman.caffeinate.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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

    suspend fun getLastDurationOnce(): DurationPreset = lastDuration.first()

    suspend fun getWidgetTimerDurationOnce(): DurationPreset.Timed = widgetTimerDuration.first()

    companion object {
        private val KEY_LAST_DURATION = stringPreferencesKey("last_duration")
        private val KEY_WIDGET_TIMER_DURATION = stringPreferencesKey("widget_timer_duration")

        private fun String?.toTimedOrDefault(): DurationPreset.Timed {
            if (this == null) return DurationPreset.DefaultTimed
            return when (val parsed = DurationPreset.fromSerialized(this)) {
                is DurationPreset.Timed -> parsed
                DurationPreset.Infinite -> DurationPreset.DefaultTimed
            }
        }
    }
}
