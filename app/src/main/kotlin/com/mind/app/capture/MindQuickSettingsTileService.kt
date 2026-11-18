package com.mind.app.capture

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.mind.app.core.model.CaptureMethod

/**
 * Android Quick Settings tile ("Save to Mind") enabling one-tap screen capture from anywhere.
 */
class MindQuickSettingsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = "Save to Mind"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = CaptureTriggerActivity.createIntent(this, CaptureMethod.QUICK_SETTINGS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_CODE_QS,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            launchLegacy(intent)
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun launchLegacy(intent: Intent) {
        startActivityAndCollapse(intent)
    }

    private companion object {
        const val REQUEST_CODE_QS = 3011
    }
}
