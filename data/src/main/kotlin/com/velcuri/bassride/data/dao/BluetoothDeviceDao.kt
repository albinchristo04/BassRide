package com.velcuri.bassride.data.dao

import androidx.room.*
import com.velcuri.bassride.data.entity.BluetoothDeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BluetoothDeviceDao {

    @Query("SELECT * FROM bluetooth_devices ORDER BY name ASC")
    fun observeAll(): Flow<List<BluetoothDeviceEntity>>

    @Query("SELECT * FROM bluetooth_devices WHERE macAddress = :macAddress")
    suspend fun getByMac(macAddress: String): BluetoothDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: BluetoothDeviceEntity)

    @Update
    suspend fun update(device: BluetoothDeviceEntity)

    @Delete
    suspend fun delete(device: BluetoothDeviceEntity)

    @Query("UPDATE bluetooth_devices SET linkedPresetId = :presetId WHERE macAddress = :macAddress")
    suspend fun linkPreset(macAddress: String, presetId: Long?)
}
