package com.velcuri.bassride.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.action.ActionParameters
import androidx.glance.unit.ColorProvider
import com.velcuri.bassride.audio.BassRideService

/**
 * Home-screen / lock-screen quick-access widget for BassRide (Pro feature).
 *
 * Shows the active preset name with Previous / Next preset navigation buttons.
 * State is stored in DataStore preferences and updated by [BassRideWidgetUpdater]
 * whenever the active preset changes in the app.
 */
class BassRideWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetContent() }
    }

    companion object {
        val PREF_PRESET_NAME = stringPreferencesKey("widget_preset_name")
        val PREF_EQ_ACTIVE   = booleanPreferencesKey("widget_eq_active")
    }
}

@Composable
private fun WidgetContent() {
    val prefs      = currentState<androidx.datastore.preferences.core.Preferences>()
    val presetName = prefs[BassRideWidget.PREF_PRESET_NAME] ?: "Flat"
    val eqActive   = prefs[BassRideWidget.PREF_EQ_ACTIVE]   ?: false

    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App label
            Text(
                text = "BassRide",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = GlanceTheme.colors.onSurface
                )
            )

            // EQ status indicator
            Text(
                text = if (eqActive) "● EQ Active" else "○ EQ Off",
                style = TextStyle(
                    fontSize = 10.sp,
                    color = ColorProvider(
                        if (eqActive)
                            androidx.compose.ui.graphics.Color(0xFF1A73E8)
                        else
                            androidx.compose.ui.graphics.Color(0xFF888888)
                    )
                ),
                modifier = GlanceModifier.padding(vertical = 4.dp)
            )

            // Active preset name (large, readable)
            Text(
                text = presetName,
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurface
                ),
                modifier = GlanceModifier.padding(vertical = 8.dp)
            )

            // Prev / Next preset navigation
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "◀  Prev",
                    style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.primary),
                    modifier = GlanceModifier
                        .clickable(actionRunCallback<PrevPresetAction>())
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Spacer(modifier = GlanceModifier.width(16.dp))
                Text(
                    text = "Next  ▶",
                    style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.primary),
                    modifier = GlanceModifier
                        .clickable(actionRunCallback<NextPresetAction>())
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Action callbacks — called when the user taps Prev / Next in the widget
// ---------------------------------------------------------------------------

class PrevPresetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        context.sendBroadcast(
            android.content.Intent(BassRideService.WIDGET_ACTION_SWITCH_PRESET).apply {
                setPackage(context.packageName)
                putExtra(BassRideService.EXTRA_WIDGET_DIRECTION, BassRideService.DIRECTION_PREV)
            }
        )
    }
}

class NextPresetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        context.sendBroadcast(
            android.content.Intent(BassRideService.WIDGET_ACTION_SWITCH_PRESET).apply {
                setPackage(context.packageName)
                putExtra(BassRideService.EXTRA_WIDGET_DIRECTION, BassRideService.DIRECTION_NEXT)
            }
        )
    }
}
