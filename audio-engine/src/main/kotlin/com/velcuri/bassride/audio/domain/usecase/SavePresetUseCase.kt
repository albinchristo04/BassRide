package com.velcuri.bassride.audio.domain.usecase

import com.velcuri.bassride.audio.domain.repository.PresetRepository
import com.velcuri.bassride.data.entity.PresetEntity
import javax.inject.Inject

private const val FREE_TIER_PRESET_LIMIT = 2

class SavePresetUseCase @Inject constructor(
    private val repository: PresetRepository
) {
    sealed class Result {
        data class Success(val presetId: Long) : Result()
        data object LimitReached : Result()
    }

    suspend operator fun invoke(preset: PresetEntity, isProUnlocked: Boolean): Result {
        val isUpdate = preset.id != 0L
        if (!isProUnlocked && !isUpdate) {
            val count = repository.countUserPresets()
            if (count >= FREE_TIER_PRESET_LIMIT) return Result.LimitReached
        }
        val id = repository.save(preset)
        return Result.Success(id)
    }
}
