package com.maciejhetman.caffeinate.session

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.maciejhetman.caffeinate.service.CaffeineService
import com.maciejhetman.caffeinate.tile.CaffeineTileService
import com.maciejhetman.caffeinate.widget.CaffeineWidget
import androidx.glance.appwidget.updateAll
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

    private suspend fun toggleInternal() {
        if (_session.value.isActive) {
            stop()
        } else {
            startInternal(prefs.getLastDurationOnce())
        }
    }

    private suspend fun startInternal(duration: DurationPreset) {
        prefs.setLastDuration(duration)
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
        _session.value = session
        notifySurfaces()
    }

    private fun notifySurfaces() {
        TileService.requestListeningState(
            appContext,
            ComponentName(appContext, CaffeineTileService::class.java),
        )
        scope.launch(Dispatchers.IO) {
            runCatching {
                CaffeineWidget().updateAll(appContext)
            }
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
