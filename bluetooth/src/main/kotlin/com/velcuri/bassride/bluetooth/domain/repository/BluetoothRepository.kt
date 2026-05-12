package com.velcuri.bassride.bluetooth.domain.repository

import com.velcuri.bassride.data.entity.BluetoothDeviceEntity
import kotlinx.coroutines.flow.Flow

interface BluetoothRepository {
    fun observeDevices(): Flow<List<BluetoothDeviceEntity>>
    suspend fun getDevice(macAddress: String): BluetoothDeviceEntity?
    suspend fun saveDevice(macAddress: String, name: String)
    suspend fun linkPreset(macAddress: String, presetId: Long?)
    suspend fun setAutoSwitch(macAddress: String, enabled: Boolean)
    suspend fun deleteDevice(macAddress: String)
}
