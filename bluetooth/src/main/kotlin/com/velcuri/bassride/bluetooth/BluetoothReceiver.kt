package com.velcuri.bassride.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.velcuri.bassride.bluetooth.domain.model.BluetoothDeviceInfo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class BluetoothEvent {
    data class DeviceConnected(val device: BluetoothDeviceInfo) : BluetoothEvent()
    data class DeviceDisconnected(val device: BluetoothDeviceInfo) : BluetoothEvent()
}

@AndroidEntryPoint
class BluetoothReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val bluetoothDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        } ?: return

        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return

        val deviceInfo = BluetoothDeviceInfo(
            macAddress = bluetoothDevice.address,
            name = bluetoothDevice.name ?: "Unknown Device"
        )

        scope.launch {
            when (action) {
                BluetoothDevice.ACTION_ACL_CONNECTED ->
                    _events.emit(BluetoothEvent.DeviceConnected(deviceInfo))
                BluetoothDevice.ACTION_ACL_DISCONNECTED ->
                    _events.emit(BluetoothEvent.DeviceDisconnected(deviceInfo))
            }
        }
    }

    companion object {
        private val _events = MutableSharedFlow<BluetoothEvent>(extraBufferCapacity = 8)
        val events: SharedFlow<BluetoothEvent> = _events.asSharedFlow()
    }
}
