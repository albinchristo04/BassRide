package com.velcuri.bassride.bluetooth.domain.usecase

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.velcuri.bassride.bluetooth.domain.model.BluetoothDeviceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class GetPairedDevicesUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(): List<BluetoothDeviceInfo> {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermission) return emptyList()

        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = manager?.adapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return adapter.bondedDevices.orEmpty().map { device ->
            BluetoothDeviceInfo(
                macAddress = device.address,
                name = device.name ?: "Unknown Device"
            )
        }
    }
}
