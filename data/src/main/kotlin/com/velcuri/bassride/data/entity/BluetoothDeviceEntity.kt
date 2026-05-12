package com.velcuri.bassride.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bluetooth_devices")
data class BluetoothDeviceEntity(
    @PrimaryKey val macAddress: String,
    val name: String,
    val linkedPresetId: Long? = null,
    val autoSwitchEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
