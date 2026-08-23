package com.maciejhetman.caffeinate.session

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.glance.appwidget.updateAll
import com.maciejhetman.caffeinate.service.CaffeineService
import com.maciejhetman.caffeinate.tile.CaffeineTileService
import com.maciejhetman.caffeinate.widget.CaffeineWidget
import com.maciejhetman.caffeinate.widget.TimerCaffeineWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Central API for starting/stopping keep-awake sessions.
 * UI, QS tile, duration dialog, and widget all go through this — never touch the wake lock directly.
 */
class CaffeineController private constructor(
    private val appContext: Context,
) {
    private val prefs = CaffeinePreferences(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _session = MutableStateFlow<CaffeineSession>(CaffeineSession.Off)
    val session: StateFlow<CaffeineSession> = _session.asStateFlow()

    val lastDuration = prefs.lastDuration
    val widgetTimerDuration = prefs.widgetTimerDuration
    val themeMode = prefs.themeMode
    val dynamicColorEnabled = prefs.dynamicColorEnabled
    val stopOnScreenOff = prefs.stopOnScreenOff

    fun start(duration: DurationPreset) {
        scope.launch { startInternal(duration) }
    }

    fun stop() {
        val intent = Intent(appContext, CaffeineService::class.java).apply {
            action = CaffeineService.ACTION_STOP
        }
        appContext.startService(intent)
    }

    fun toggle() {
        scope.launch { toggleInternal() }
    }

    /** Blocking helper for Glance action callbacks. */
    fun toggleBlocking() {
        runBlocking { toggleInternal() }
    }

    /**
     * Blocking helper for the Caffeinate (infinite) widget tap.
     * - Off -> starts an infinite session.
     * - Infinite session running -> stops it.
     * - Timed session running -> switches it to infinite without stopping.
     */
    fun toggleInfiniteBlocking() {
        runBlocking {
            val current = _session.value
            if (current is CaffeineSession.On && current.duration is DurationPreset.Infinite) {
                stop()
            } else {
                startInternal(DurationPreset.Infinite)
            }
        }
    }

    /**
     * Blocking helper for the Timer widget tap, using the timer-widget duration from settings.
     * - Off -> starts a timed session.
     * - Timed session running -> stops it.
     * - Infinite session running -> switches it to timed without stopping.
     */
    fun toggleWidgetTimerBlocking() {
        runBlocking {
            val current = _session.value
            if (current is CaffeineSession.On && current.duration.isTimed) {
                stop()
            } else {
                startInternal(prefs.getWidgetTimerDurationOnce())
            }
        }
    }

    fun startWithLastDuration() {
        scope.launch {
            startInternal(prefs.getLastDurationOnce())
        }
    }

    fun setLastDuration(duration: DurationPreset) {
        scope.launch {
            prefs.setLastDuration(duration)
        }
    }

    fun setWidgetTimerDuration(duration: DurationPreset.Timed) {
        scope.launch {
            prefs.setWidgetTimerDuration(duration)
            runCatching { TimerCaffeineWidget().updateAll(appContext) }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        scope.launch { prefs.setThemeMode(mode) }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        scope.launch { prefs.setDynamicColorEnabled(enabled) }
    }

    fun setStopOnScreenOff(enabled: Boolean) {
        scope.launch { prefs.setStopOnScreenOff(enabled) }
    }

    private suspend fun toggleInternal() {
        if (_session.value.isActive) {
            stop()
        } else {
            startInternal(prefs.getLastDurationOnce())
        }
    }

    private suspend fun startInternal(duration: DurationPreset) {
        prefs.setLastDuration(duration)
        // Optimistic On so surfaces update before the foreground service binds.
        publishSession(
            CaffeineSession.On(
                duration = duration,
                remainingMillis = duration.millis,
                endsAtEpochMillis = duration.millis?.let { System.currentTimeMillis() + it },
            ),
        )
        val intent = Intent(appContext, CaffeineService::class.java).apply {
            action = CaffeineService.ACTION_START
            putExtra(CaffeineService.EXTRA_DURATION, duration.serialize())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }
    }

    internal fun publishSession(session: CaffeineSession) {
        val previous = _session.value
        _session.value = session
        val activeChanged = previous.isActive != session.isActive
        // Tile collects while listening; only rebind / refresh widgets when on↔off flips.
        // Per-second countdown ticks must not hammer requestListeningState (breaks tile UI).
        // While a Glance composition is alive, collectAsState still picks up ticks.
        if (activeChanged) {
            notifySurfaces()
        }
    }

    private fun notifySurfaces() {
        TileService.requestListeningState(
            appContext,
            ComponentName(appContext, CaffeineTileService::class.java),
        )
        scope.launch(Dispatchers.IO) {
            runCatching { CaffeineWidget().updateAll(appContext) }
            runCatching { TimerCaffeineWidget().updateAll(appContext) }
        }
    }

    companion object {
        @Volatile
        private var instance: CaffeineController? = null

        fun get(context: Context): CaffeineController {
            return instance ?: synchronized(this) {
                instance ?: CaffeineController(context.applicationContext).also { instance = it }
            }
        }
    }
}
