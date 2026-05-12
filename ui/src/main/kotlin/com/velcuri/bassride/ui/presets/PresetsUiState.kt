package com.velcuri.bassride.ui.presets

import com.velcuri.bassride.data.entity.PresetEntity

private const val FREE_PRESET_LIMIT = 2

sealed class PresetsUiState {
    data object Loading : PresetsUiState()
    data class Ready(
        val presets: List<PresetEntity>,
        val isProUnlocked: Boolean,
        val customPresetCount: Int = 0
    ) : PresetsUiState() {
        /** Shown to free-tier users as a "1 / 2 presets used" indicator. */
        val presetSlotLabel: String get() = "$customPresetCount / $FREE_PRESET_LIMIT custom presets used"
        val isAtFreeLimit: Boolean get() = !isProUnlocked && customPresetCount >= FREE_PRESET_LIMIT
    }
}
