package com.velcuri.bassride.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velcuri.bassride.audio.domain.usecase.LoadPresetsUseCase
import com.velcuri.bassride.bluetooth.domain.repository.BluetoothRepository
import com.velcuri.bassride.bluetooth.domain.usecase.GetPairedDevicesUseCase
import com.velcuri.bassride.bluetooth.domain.usecase.LinkPresetToDeviceUseCase
import com.velcuri.bassride.bluetooth.domain.usecase.SaveDeviceProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val bluetoothRepository: BluetoothRepository,
    private val loadPresetsUseCase: LoadPresetsUseCase,
    private val linkPresetToDeviceUseCase: LinkPresetToDeviceUseCase,
    private val saveDeviceProfileUseCase: SaveDeviceProfileUseCase,
    private val getPairedDevicesUseCase: GetPairedDevicesUseCase
) : ViewModel() {

    private val _importCandidates = MutableStateFlow<List<com.velcuri.bassride.bluetooth.domain.model.BluetoothDeviceInfo>>(emptyList())

    val uiState: StateFlow<DevicesUiState> = combine(
        bluetoothRepository.observeDevices(),
        loadPresetsUseCase(),
        _importCandidates
    ) { devices, presets, candidates ->
        // Exclude already-saved devices from the import candidate list
        val savedMacs = devices.map { it.macAddress }.toSet()
        val filteredCandidates = candidates.filter { it.macAddress !in savedMacs }
        DevicesUiState.Ready(
            devices = devices,
            presets = presets,
            importCandidates = filteredCandidates
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DevicesUiState.Loading)

    /** Reads system-paired BT devices and exposes them as import candidates. */
    fun refreshPairedDevices() {
        _importCandidates.value = getPairedDevicesUseCase()
    }

    /** Saves a paired device from the import list into the DB. */
    fun importDevice(macAddress: String, name: String) {
        viewModelScope.launch {
            saveDeviceProfileUseCase(macAddress, name)
            // The DB Flow will automatically remove it from importCandidates via combine
        }
    }

    fun linkPreset(macAddress: String, presetId: Long?) {
        viewModelScope.launch {
            linkPresetToDeviceUseCase(macAddress, presetId)
        }
    }

    fun setAutoSwitch(macAddress: String, enabled: Boolean) {
        viewModelScope.launch {
            bluetoothRepository.setAutoSwitch(macAddress, enabled)
        }
    }

    fun deleteDevice(macAddress: String) {
        viewModelScope.launch {
            bluetoothRepository.deleteDevice(macAddress)
        }
    }
}
