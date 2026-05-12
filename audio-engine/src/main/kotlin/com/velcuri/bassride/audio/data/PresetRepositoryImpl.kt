package com.velcuri.bassride.audio.data

import com.velcuri.bassride.audio.domain.repository.PresetRepository
import com.velcuri.bassride.data.dao.PresetDao
import com.velcuri.bassride.data.entity.PresetEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PresetRepositoryImpl @Inject constructor(
    private val dao: PresetDao
) : PresetRepository {

    override fun observeAll(): Flow<List<PresetEntity>> = dao.observeAll()

    override suspend fun getById(id: Long): PresetEntity? = dao.getById(id)

    override suspend fun save(preset: PresetEntity): Long = dao.upsert(preset)

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun countUserPresets(): Int = dao.countUserPresets()
}
