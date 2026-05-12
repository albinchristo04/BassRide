package com.velcuri.bassride.data.di

import android.content.Context
import androidx.room.Room
import com.velcuri.bassride.data.dao.BluetoothDeviceDao
import com.velcuri.bassride.data.dao.PresetDao
import com.velcuri.bassride.data.dao.UserSettingsDao
import com.velcuri.bassride.data.db.BassRideDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BassRideDatabase =
        Room.databaseBuilder(
            context,
            BassRideDatabase::class.java,
            BassRideDatabase.DATABASE_NAME
        ).build()

    @Provides
    fun provideBluetoothDeviceDao(db: BassRideDatabase): BluetoothDeviceDao = db.bluetoothDeviceDao()

    @Provides
    fun providePresetDao(db: BassRideDatabase): PresetDao = db.presetDao()

    @Provides
    fun provideUserSettingsDao(db: BassRideDatabase): UserSettingsDao = db.userSettingsDao()
}
