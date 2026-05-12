package com.velcuri.bassride.data.dao

import androidx.room.*
import com.velcuri.bassride.data.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {

    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun observe(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun get(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: UserSettingsEntity)

    @Query("UPDATE user_settings SET isProUnlockedCached = :isPro WHERE id = 1")
    suspend fun setProUnlocked(isPro: Boolean)

    @Query("UPDATE user_settings SET autoSwitchEnabled = :enabled WHERE id = 1")
    suspend fun setAutoSwitchEnabled(enabled: Boolean)

    @Query("UPDATE user_settings SET hasCompletedOnboarding = 1 WHERE id = 1")
    suspend fun markOnboardingComplete()
}
