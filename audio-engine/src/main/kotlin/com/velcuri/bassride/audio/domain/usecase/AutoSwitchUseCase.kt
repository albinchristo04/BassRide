package com.velcuri.bassride.audio.domain.usecase

import com.velcuri.bassride.bluetooth.domain.repository.BluetoothRepository
import com.velcuri.bassride.data.dao.PresetDao
import com.velcuri.bassride.data.dao.UserSettingsDao
import javax.inject.Inject

/**
 * Encapsulates the full auto-switch logic: given a connected device MAC address, checks the
 * global auto-switch preference, the per-device toggle, looks up the linked preset, and
 * applies it via [ApplyPresetUseCase].
 *
 * Returns a [Result] describing what happened so the caller can update the notification / widget.
 */
class AutoSwitchUseCase @Inject constructor(
    private val userSettingsDao: UserSettingsDao,
    private val bluetoothRepository: BluetoothRepository,
    private val presetDao: PresetDao,
    private val applyPresetUseCase: ApplyPresetUseCase
) {
    sealed class Result {
        /** A preset was successfully found and applied. */
        data class Applied(val presetId: Long, val presetName: String) : Result()
        /** The device is unknown — caller should save it and notify the user. */
        data object DeviceUnknown : Result()
        /** Auto-switch is disabled globally or per-device. */
        data object AutoSwitchDisabled : Result()
        /** The device is known but has no linked preset. */
        data object NoPresetLinked : Result()
    }

    suspend operator fun invoke(macAddress: String): Result {
        // 1. Check the global auto-switch toggle (Settings screen)
        val globalSettings = userSettingsDao.get()
        if (globalSettings?.autoSwitchEnabled == false) return Result.AutoSwitchDisabled

        // 2. Check per-device existence and toggle
        val device = bluetoothRepository.getDevice(macAddress)
            ?: return Result.DeviceUnknown

        if (!device.autoSwitchEnabled) return Result.AutoSwitchDisabled

        // 3. Resolve linked preset
        val presetId = device.linkedPresetId ?: return Result.NoPresetLinked
        val preset = presetDao.getById(presetId) ?: return Result.NoPresetLinked

        // 4. Apply
        applyPresetUseCase(preset)
        return Result.Applied(preset.id, preset.name)
    }
}
