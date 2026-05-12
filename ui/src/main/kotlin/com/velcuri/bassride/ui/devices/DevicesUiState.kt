package com.velcuri.bassride.ui.devices

import com.velcuri.bassride.bluetooth.domain.model.BluetoothDeviceInfo
import com.velcuri.bassride.data.entity.BluetoothDeviceEntity
import com.velcuri.bassride.data.entity.PresetEntity

sealed class DevicesUiState {
    data object Loading : DevicesUiState()
    data class Ready(
        val devices: List<BluetoothDeviceEntity>,
        val presets: List<PresetEntity>,
        /** System-paired devices not yet in the DB — shown as import candidates */
        val importCandidates: List<BluetoothDeviceInfo> = emptyList()
    ) : DevicesUiState()
}
