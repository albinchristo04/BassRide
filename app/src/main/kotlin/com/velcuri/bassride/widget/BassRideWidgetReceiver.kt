package com.velcuri.bassride.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * BroadcastReceiver entry-point for the BassRide home-screen widget.
 * Registered in AndroidManifest.xml with the appwidget-provider XML metadata.
 */
class BassRideWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BassRideWidget()
}
