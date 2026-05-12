package com.velcuri.bassride.bluetooth.data

import com.velcuri.bassride.bluetooth.domain.repository.BluetoothRepository
import com.velcuri.bassride.data.dao.BluetoothDeviceDao
import com.velcuri.bassride.data.entity.BluetoothDeviceEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BluetoothRepositoryImpl @Inject constructor(
    private val dao: BluetoothDeviceDao
) : BluetoothRepository {

    override fun observeDevices(): Flow<List<BluetoothDeviceEntity>> = dao.observeAll()

    override suspend fun getDevice(macAddress: String): BluetoothDeviceEntity? =
        dao.getByMac(macAddress)

    override suspend fun saveDevice(macAddress: String, name: String) {
        val existing = dao.getByMac(macAddress)
        if (existing == null) {
            dao.upsert(BluetoothDeviceEntity(macAddress = macAddress, name = name))
        }
    }

    override suspend fun linkPreset(macAddress: String, presetId: Long?) {
        dao.linkPreset(macAddress, presetId)
    }

    override suspend fun setAutoSwitch(macAddress: String, enabled: Boolean) {
        val device = dao.getByMac(macAddress) ?: return
        dao.update(device.copy(autoSwitchEnabled = enabled))
    }

    override suspend fun deleteDevice(macAddress: String) {
        val device = dao.getByMac(macAddress) ?: return
        dao.delete(device)
    }
}
