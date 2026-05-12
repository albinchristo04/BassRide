package com.velcuri.bassride.data.seeder

import com.velcuri.bassride.data.dao.PresetDao
import com.velcuri.bassride.data.dao.UserSettingsDao
import com.velcuri.bassride.data.entity.PresetEntity
import com.velcuri.bassride.data.entity.UserSettingsEntity
import javax.inject.Inject

class DatabaseSeeder @Inject constructor(
    private val presetDao: PresetDao,
    private val userSettingsDao: UserSettingsDao
) {
    suspend fun seedIfNeeded() {
        if (userSettingsDao.get() == null) {
            userSettingsDao.upsert(UserSettingsEntity())
            seedBuiltInPresets()
        }
    }

    private suspend fun seedBuiltInPresets() {
        val builtIns = listOf(
            PresetEntity(name = "Flat", isBuiltIn = true),
            PresetEntity(
                name = "Bass Boost",
                isBuiltIn = true,
                band0 = 600, band1 = 500, band2 = 300, band3 = 150
            ),
            PresetEntity(
                name = "Vocal Clarity",
                isBuiltIn = true,
                band3 = -200, band4 = 200, band5 = 400, band6 = 300
            ),
            PresetEntity(
                name = "Road Noise",
                isBuiltIn = true,
                band0 = 800, band1 = 600, band2 = 200, band7 = 200, band8 = 300
            )
        )
        builtIns.forEach { presetDao.upsert(it) }
    }
}
