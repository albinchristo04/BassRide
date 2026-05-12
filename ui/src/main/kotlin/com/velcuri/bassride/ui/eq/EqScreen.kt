package com.velcuri.bassride.ui.eq

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.velcuri.bassride.data.entity.PresetEntity
import com.velcuri.bassride.ui.components.BandSlider
import com.velcuri.bassride.ui.components.DbRuler
import com.velcuri.bassride.ui.theme.BassRideBackground
import com.velcuri.bassride.ui.theme.BassRideCyan
import com.velcuri.bassride.ui.theme.BassRideOnSurface
import com.velcuri.bassride.ui.theme.BassRideOnSurfaceVariant
import com.velcuri.bassride.ui.theme.BassRideOutline
import com.velcuri.bassride.ui.theme.BassRidePrimary
import com.velcuri.bassride.ui.theme.BassRidePrimaryContainer
import com.velcuri.bassride.ui.theme.BassRidePrimaryLight
import com.velcuri.bassride.ui.theme.BassRideSurface
import com.velcuri.bassride.ui.theme.BassRideSurfaceHigh
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqScreen(
    onUpgradeClick: () -> Unit,
    viewModel: EqViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    var showSaveDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = BassRideBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = uiState) {
            is EqUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = BassRidePrimary)
                    Text(
                        "Initializing EQ\u2026",
                        color = BassRideOnSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            is EqUiState.Error -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = BassRidePrimary
                    )
                    Text(
                        "EQ Unavailable",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BassRideOnSurface
                    )
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = BassRideOnSurfaceVariant
                    )
                    Button(
                        onClick = { viewModel.retryInitialization() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BassRidePrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Retry") }
                }
            }

            is EqUiState.Ready -> EqContent(
                state = state,
                onBandChanged = viewModel::onBandChanged,
                onPresetSelected = viewModel::onPresetSelected,
                onSavePreset = { showSaveDialog = true },
                onReset = { viewModel.resetToFlat() },
                onBassBoostToggle = { enabled -> viewModel.setBassBoostEnabled(enabled) },
                onBassBoostStrength = { strength -> viewModel.setBassBoostEnabled(true, strength) },
                onVirtualizerToggle = { enabled -> viewModel.setVirtualizerEnabled(enabled) },
                onUpgradeClick = onUpgradeClick,
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (showSaveDialog) {
        SavePresetDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                viewModel.saveCurrentAsPreset(name)
                showSaveDialog = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EqContent(
    state: EqUiState.Ready,
    onBandChanged: (Int, Int) -> Unit,
    onPresetSelected: (PresetEntity) -> Unit,
    onSavePreset: () -> Unit,
    onReset: () -> Unit,
    onBassBoostToggle: (Boolean) -> Unit,
    onBassBoostStrength: (Int) -> Unit,
    onVirtualizerToggle: (Boolean) -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleBands = if (state.isProUnlocked) state.bandCount else minOf(state.bandCount, 5)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {

        // ── Band mode header ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "EQUALIZER",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = BassRideOnSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BandModeChip(label = "5-BAND", active = true, color = BassRideCyan)
                Surface(
                    onClick = { if (!state.isProUnlocked) onUpgradeClick() },
                    shape = RoundedCornerShape(20.dp),
                    color = if (state.isProUnlocked) BassRidePrimary.copy(alpha = 0.2f)
                            else Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        if (state.isProUnlocked) BassRidePrimary else BassRideOutline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!state.isProUnlocked) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = BassRideOnSurfaceVariant
                            )
                        }
                        Text(
                            text = "PRO 10-BAND",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.isProUnlocked) BassRidePrimary
                                    else BassRideOnSurfaceVariant
                        )
                    }
                }
            }
        }

        // ── Preset selector card ─────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = BassRideSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "CURRENT PRESET",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    letterSpacing = 1.5.sp,
                    color = BassRideOnSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Equalizer,
                        contentDescription = null,
                        tint = BassRidePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = state.activePreset?.name ?: "Custom",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BassRideOnSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(36.dp)
                            .semantics { contentDescription = "Favorite" }
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = BassRideOnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onSavePreset,
                        modifier = Modifier
                            .size(36.dp)
                            .semantics { contentDescription = "Save preset" }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = BassRideOnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── EQ sliders card ──────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(280.dp),
            colors = CardDefaults.cardColors(containerColor = BassRideSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)
            ) {
                DbRuler(
                    minMillibels = state.minMillibels,
                    maxMillibels = state.maxMillibels,
                    modifier = Modifier.fillMaxHeight()
                )
                Spacer(Modifier.width(4.dp))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(visibleBands) { i ->
                        BandSlider(
                            bandIndex = i,
                            levelMillibels = state.bandLevels.getOrElse(i) { 0 },
                            minMillibels = state.minMillibels,
                            maxMillibels = state.maxMillibels,
                            onLevelChange = { level -> onBandChanged(i, level) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (!state.isProUnlocked && state.bandCount > 5) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onUpgradeClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Unlock Pro",
                                    tint = BassRideOnSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Pro",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BassRideOnSurfaceVariant,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Action buttons ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onSavePreset,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BassRideCyan,
                    contentColor = BassRideBackground
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "SAVE PRESET",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.8.sp
                )
            }
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BassRideOutline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BassRideOnSurfaceVariant
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("RESET", fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Built-in presets ─────────────────────────────────────────────────
        SectionLabel("BUILT-IN PRESETS")
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.presets.filter { it.isBuiltIn }.forEach { preset ->
                PresetChip(
                    name = preset.name,
                    isActive = state.activePreset?.id == preset.id,
                    isLocked = false,
                    onClick = { onPresetSelected(preset) }
                )
            }
        }

        // ── Custom presets ───────────────────────────────────────────────────
        val customPresets = state.presets.filter { !it.isBuiltIn }
        if (customPresets.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabelInline(
                    text = "MY PRESETS",
                    modifier = Modifier.weight(1f)
                )
                if (!state.isProUnlocked) {
                    val count = customPresets.size
                    Text(
                        text = "$count / 2",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = if (count >= 2) MaterialTheme.colorScheme.error
                                else BassRideOnSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                customPresets.forEachIndexed { idx, preset ->
                    val locked = !state.isProUnlocked && idx >= 2
                    PresetChip(
                        name = preset.name,
                        isActive = state.activePreset?.id == preset.id,
                        isLocked = locked,
                        onClick = { if (locked) onUpgradeClick() else onPresetSelected(preset) }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Effects ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabelInline(text = "EFFECTS", modifier = Modifier.weight(1f))
            if (!state.isProUnlocked) ProBadge()
        }
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = BassRideSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                EffectRow(
                    label = "Bass Booster",
                    icon = Icons.Default.VolumeUp,
                    isProUnlocked = state.isProUnlocked,
                    enabled = state.isBassBoostEnabled,
                    onToggle = onBassBoostToggle,
                    onUpgradeClick = onUpgradeClick
                )
                if (state.isBassBoostEnabled && state.isProUnlocked) {
                    Slider(
                        value = state.bassBoostStrength.toFloat(),
                        onValueChange = { onBassBoostStrength(it.roundToInt()) },
                        valueRange = 0f..1000f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = BassRidePrimary,
                            activeTrackColor = BassRidePrimary,
                            inactiveTrackColor = BassRideSurfaceHigh
                        )
                    )
                }
                HorizontalDivider(color = BassRideOutline, modifier = Modifier.padding(vertical = 2.dp))
                EffectRow(
                    label = "Virtualizer",
                    icon = Icons.Default.MusicNote,
                    isProUnlocked = state.isProUnlocked,
                    enabled = state.isVirtualizerEnabled,
                    onToggle = onVirtualizerToggle,
                    onUpgradeClick = onUpgradeClick
                )
            }
        }

        // ── Pro upgrade banner ───────────────────────────────────────────────
        if (!state.isProUnlocked) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { onUpgradeClick() },
                colors = CardDefaults.cardColors(containerColor = BassRidePrimaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = BassRidePrimaryLight,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Unlock BassRide Pro",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = BassRideOnSurface
                        )
                        Text(
                            "10-band EQ \u00b7 Unlimited presets \u00b7 Auto-switch",
                            style = MaterialTheme.typography.bodySmall,
                            color = BassRidePrimaryLight
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = BassRidePrimaryLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Small reusable components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BandModeChip(label: String, active: Boolean, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (active) color.copy(alpha = 0.15f) else Color.Transparent,
        border = BorderStroke(1.dp, if (active) color.copy(alpha = 0.6f) else BassRideOutline)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (active) color else BassRideOnSurfaceVariant
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 9.sp,
        letterSpacing = 1.5.sp,
        color = BassRideOnSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
}

@Composable
private fun SectionLabelInline(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 9.sp,
        letterSpacing = 1.5.sp,
        color = BassRideOnSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun ProBadge() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = BassRidePrimary.copy(alpha = 0.18f)
    ) {
        Text(
            text = "PRO",
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = BassRidePrimary
        )
    }
}

@Composable
private fun PresetChip(
    name: String,
    isActive: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) BassRidePrimary else BassRideSurface,
        border = if (!isActive) BorderStroke(1.dp, BassRideOutline) else null,
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isLocked) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Pro required",
                    tint = BassRideOnSurfaceVariant,
                    modifier = Modifier.size(11.dp)
                )
            } else {
                Icon(
                    Icons.Default.Equalizer,
                    contentDescription = null,
                    tint = if (isActive) Color.White else BassRideOnSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isLocked -> BassRideOnSurfaceVariant
                    isActive -> Color.White
                    else     -> BassRideOnSurface
                }
            )
        }
    }
}

@Composable
private fun EffectRow(
    label: String,
    icon: ImageVector,
    isProUnlocked: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onUpgradeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = BassRideOnSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = BassRideOnSurface,
            modifier = Modifier.weight(1f)
        )
        if (!isProUnlocked) {
            TextButton(
                onClick = onUpgradeClick,
                modifier = Modifier.semantics { contentDescription = "Unlock $label with Pro" }
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Pro", style = MaterialTheme.typography.labelSmall)
            }
        } else {
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BassRidePrimary,
                    uncheckedTrackColor = BassRideSurfaceHigh
                ),
                modifier = Modifier.semantics {
                    contentDescription = "$label ${if (enabled) "on" else "off"}"
                }
            )
        }
    }
}

@Composable
private fun SavePresetDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BassRideSurface,
        title = {
            Text(
                "Save Preset",
                color = BassRideOnSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Preset name", color = BassRideOnSurfaceVariant) },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name.trim()) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BassRidePrimary)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = BassRideOnSurfaceVariant)
            }
        }
    )
}
