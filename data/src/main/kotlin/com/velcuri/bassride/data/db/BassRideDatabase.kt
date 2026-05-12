package com.velcuri.bassride.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.velcuri.bassride.data.dao.BluetoothDeviceDao
import com.velcuri.bassride.data.dao.PresetDao
import com.velcuri.bassride.data.dao.UserSettingsDao
import com.velcuri.bassride.data.entity.BluetoothDeviceEntity
import com.velcuri.bassride.data.entity.PresetEntity
import com.velcuri.bassride.data.entity.UserSettingsEntity

@Database(
    entities = [
        BluetoothDeviceEntity::class,
        PresetEntity::class,
        UserSettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class BassRideDatabase : RoomDatabase() {
    abstract fun bluetoothDeviceDao(): BluetoothDeviceDao
    abstract fun presetDao(): PresetDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        const val DATABASE_NAME = "bassride.db"
    }
}
