package com.priyanshu.aura.audio

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class AuraQuickTileService : TileService() {

    override fun onClick() {
        super.onClick()
        
        // Update tile to active state
        val tile = qsTile
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()

        // Collapse notification panel
        val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        sendBroadcast(it)

        // Launch CaptureActivity
        val intent = Intent(this, CaptureActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivityAndCollapse(intent)
        
        // Reset tile state after a short delay
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
