package com.velcuri.bassride.audio.domain.repository

import com.velcuri.bassride.data.entity.PresetEntity
import kotlinx.coroutines.flow.Flow

interface PresetRepository {
    fun observeAll(): Flow<List<PresetEntity>>
    suspend fun getById(id: Long): PresetEntity?
    suspend fun save(preset: PresetEntity): Long
    suspend fun delete(id: Long)
    suspend fun countUserPresets(): Int
}
