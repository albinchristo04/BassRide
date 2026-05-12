package com.velcuri.bassride.bluetooth.domain.usecase

import com.velcuri.bassride.bluetooth.domain.repository.BluetoothRepository
import com.velcuri.bassride.data.entity.BluetoothDeviceEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDeviceProfilesUseCase @Inject constructor(
    private val repository: BluetoothRepository
) {
    operator fun invoke(): Flow<List<BluetoothDeviceEntity>> = repository.observeDevices()
}
