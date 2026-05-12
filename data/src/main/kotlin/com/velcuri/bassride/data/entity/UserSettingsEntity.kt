package com.velcuri.bassride.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val autoSwitchEnabled: Boolean = true,
    val defaultPresetId: Long? = null,
    val isProUnlockedCached: Boolean = false,
    val hasCompletedOnboarding: Boolean = false
)
