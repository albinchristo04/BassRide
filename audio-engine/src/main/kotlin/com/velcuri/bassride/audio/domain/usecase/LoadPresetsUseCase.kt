package com.velcuri.bassride.audio.domain.usecase

import com.velcuri.bassride.audio.domain.repository.PresetRepository
import com.velcuri.bassride.data.entity.PresetEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoadPresetsUseCase @Inject constructor(
    private val repository: PresetRepository
) {
    operator fun invoke(): Flow<List<PresetEntity>> = repository.observeAll()
}
