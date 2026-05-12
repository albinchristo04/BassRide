package com.velcuri.bassride.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.velcuri.bassride.audio.BassRideService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Utility object for pushing state updates to the home-screen widget.
 *
 * Call [updateWidget] from the app (e.g., a WidgetUpdateReceiver registered in MainActivity
 * that listens for [BassRideService.ACTION_WIDGET_UPDATE] broadcasts).
 */
object BassRideWidgetUpdater {

    /**
     * Pushes [presetName] + [eqActive] into every active widget's DataStore preferences
     * and triggers a Glance UI update.
     */
    suspend fun updateWidget(context: Context, presetName: String, eqActive: Boolean) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(BassRideWidget::class.java)
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[BassRideWidget.PREF_PRESET_NAME] = presetName
                prefs[BassRideWidget.PREF_EQ_ACTIVE]   = eqActive
            }
            BassRideWidget().update(context, glanceId)
        }
    }
}

/**
 * Receives [BassRideService.ACTION_WIDGET_UPDATE] broadcasts from the service and
 * calls [BassRideWidgetUpdater.updateWidget] to refresh the Glance widget state.
 */
@AndroidEntryPoint
class WidgetUpdateReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BassRideService.ACTION_WIDGET_UPDATE) return
        val presetName = intent.getStringExtra(BassRideService.EXTRA_PRESET_NAME) ?: return
        val eqActive   = intent.getBooleanExtra(BassRideService.EXTRA_EQ_ACTIVE, false)

        scope.launch {
            BassRideWidgetUpdater.updateWidget(context, presetName, eqActive)
        }
    }
}

/**
 * Receives Prev/Next preset taps from the widget buttons and forwards them to the service.
 */
@AndroidEntryPoint
class WidgetPresetSwitchReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BassRideService.WIDGET_ACTION_SWITCH_PRESET) return
        val direction = intent.getStringExtra(BassRideService.EXTRA_WIDGET_DIRECTION) ?: return

        val serviceIntent = Intent(context, BassRideService::class.java).apply {
            action = BassRideService.WIDGET_ACTION_SWITCH_PRESET
            putExtra(BassRideService.EXTRA_WIDGET_DIRECTION, direction)
        }
        context.startService(serviceIntent)
    }
}

