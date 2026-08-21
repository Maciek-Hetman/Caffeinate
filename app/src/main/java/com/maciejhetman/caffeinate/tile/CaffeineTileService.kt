package com.maciejhetman.caffeinate.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.maciejhetman.caffeinate.R
import com.maciejhetman.caffeinate.session.CaffeineController
import com.maciejhetman.caffeinate.session.CaffeineSession

/**
 * Quick Settings tile: tap toggles with last duration; long-press opens duration prefs activity.
 */
class CaffeineTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        CaffeineController.get(this).toggle()
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val session = CaffeineController.get(this).session.value
        when (session) {
            CaffeineSession.Off -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.tile_label)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = getString(R.string.status_off)
                }
            }
            is CaffeineSession.On -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.tile_label)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = session.displayRemaining()
                }
            }
        }
        tile.updateTile()
    }
}
