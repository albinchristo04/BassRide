package com.velcuri.bassride.bluetooth.domain.usecase

import com.velcuri.bassride.bluetooth.domain.repository.BluetoothRepository
import javax.inject.Inject

class SaveDeviceProfileUseCase @Inject constructor(
    private val repository: BluetoothRepository
) {
    suspend operator fun invoke(macAddress: String, name: String) {
        repository.saveDevice(macAddress, name)
    }
}
