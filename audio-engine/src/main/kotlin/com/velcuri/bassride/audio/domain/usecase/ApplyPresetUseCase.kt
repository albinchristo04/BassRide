package com.velcuri.bassride.audio.domain.usecase

import com.velcuri.bassride.audio.BassRideDspEngine
import com.velcuri.bassride.data.entity.PresetEntity
import javax.inject.Inject

class ApplyPresetUseCase @Inject constructor(
    private val eqEngine: BassRideDspEngine
) {
    suspend operator fun invoke(preset: PresetEntity) {
        eqEngine.applyPreset(preset)
    }
}
