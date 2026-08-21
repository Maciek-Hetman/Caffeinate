package com.maciejhetman.caffeinate.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.maciejhetman.caffeinate.R
import com.maciejhetman.caffeinate.session.CaffeineController
import com.maciejhetman.caffeinate.session.CaffeineSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Quick Settings tile: tap toggles with last duration; long-press opens duration prefs activity.
 */
class CaffeineTileService : TileService() {

    private var listenScope: CoroutineScope? = null
    private var collectJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        listenScope = scope
        collectJob = scope.launch {
            CaffeineController.get(this@CaffeineTileService).session.collectLatest { session ->
                applySession(session)
            }
        }
    }

    override fun onStopListening() {
        collectJob?.cancel()
        collectJob = null
        listenScope?.cancel()
        listenScope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val controller = CaffeineController.get(this)
        val turningOn = !controller.session.value.isActive
        // Optimistic UI so the tile flips immediately while the service starts.
        qsTile?.let { tile ->
            tile.state = if (turningOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = getString(R.string.tile_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(
                    if (turningOn) R.string.status_on else R.string.status_off,
                )
            }
            tile.updateTile()
        }
        controller.toggle()
    }

    private fun applySession(session: CaffeineSession) {
        val tile = qsTile ?: return
        val newState = when (session) {
            CaffeineSession.Off -> Tile.STATE_INACTIVE
            is CaffeineSession.On -> Tile.STATE_ACTIVE
        }
        val newSubtitle = when (session) {
            CaffeineSession.Off -> getString(R.string.status_off)
            is CaffeineSession.On -> session.displayRemaining()
        }
        val stateChanged = tile.state != newState
        val subtitleChanged = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            tile.subtitle?.toString() != newSubtitle

        if (!stateChanged && !subtitleChanged) return

        // Only flip state when it changes — avoids replaying QS activate animations.
        if (stateChanged) {
            tile.state = newState
        }
        tile.label = getString(R.string.tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = newSubtitle
        }
        tile.updateTile()
    }
}
