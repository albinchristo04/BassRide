package com.velcuri.bassride.ui.eq

import com.velcuri.bassride.data.entity.PresetEntity

sealed class EqUiState {
    data object Loading : EqUiState()
    data class Ready(
        val bandLevels: IntArray,
        val bandCount: Int,
        val minMillibels: Int,
        val maxMillibels: Int,
        val activePreset: PresetEntity?,
        val presets: List<PresetEntity>,
        val isProUnlocked: Boolean,
        val isBassBoostEnabled: Boolean = false,
        val bassBoostStrength: Int = 0,
        val isVirtualizerEnabled: Boolean = false
    ) : EqUiState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Ready) return false
            return bandLevels.contentEquals(other.bandLevels) &&
                bandCount == other.bandCount &&
                activePreset == other.activePreset &&
                presets == other.presets &&
                isProUnlocked == other.isProUnlocked
        }
        override fun hashCode(): Int = bandLevels.contentHashCode()
    }
    data class Error(val message: String) : EqUiState()
}
