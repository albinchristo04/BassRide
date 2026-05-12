package com.velcuri.bassride.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── BassRide Brand Palette ─────────────────────────────────────────────────────
val BassRideBackground       = Color(0xFF0D0E1B)
val BassRideSurface          = Color(0xFF161729)
val BassRideSurfaceHigh      = Color(0xFF1E2038)
val BassRidePrimary          = Color(0xFF7C3AED)
val BassRidePrimaryLight     = Color(0xFF8B5CF6)
val BassRidePrimaryContainer = Color(0xFF3B1A8A)
val BassRideCyan             = Color(0xFF06B6D4)
val BassRideGreen            = Color(0xFF10B981)
val BassRideOnSurface        = Color(0xFFF1F5F9)
val BassRideOnSurfaceVariant = Color(0xFF94A3B8)
val BassRideOutline          = Color(0xFF2E3050)

private val DarkColorScheme = darkColorScheme(
    primary              = BassRidePrimary,
    onPrimary            = Color.White,
    primaryContainer     = BassRidePrimaryContainer,
    onPrimaryContainer   = BassRidePrimaryLight,
    secondary            = BassRideCyan,
    onSecondary          = BassRideBackground,
    tertiary             = BassRideGreen,
    background           = BassRideBackground,
    onBackground         = BassRideOnSurface,
    surface              = BassRideSurface,
    onSurface            = BassRideOnSurface,
    surfaceVariant       = BassRideSurfaceHigh,
    onSurfaceVariant     = BassRideOnSurfaceVariant,
    outline              = BassRideOutline,
    error                = Color(0xFFEF4444),
    onError              = Color.White,
    surfaceContainerHigh = BassRideSurfaceHigh,
    surfaceContainer     = BassRideSurface,
    surfaceContainerLow  = BassRideBackground,
)

@Composable
fun BassRideTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
