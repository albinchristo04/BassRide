package com.velcuri.bassride.bluetooth.domain.usecase

import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Returns MAC addresses of Bluetooth devices currently connected on the A2DP (audio) profile.
 *
 * Uses [BluetoothAdapter.getProfileProxy] to obtain the A2DP service proxy — which is the
 * officially supported way to query classic-BT profile connection state at API 29+.
 *
 * Suspends until the proxy reports its list (or 3 s timeout), then automatically closes it.
 */
class GetConnectedA2dpDevicesUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(): List<String> = withContext(Dispatchers.IO) {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = manager?.adapter ?: return@withContext emptyList()
        if (!adapter.isEnabled) return@withContext emptyList()

        val deferred = CompletableDeferred<List<String>>()

        val listener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                val macs = proxy.connectedDevices.map { it.address }
                deferred.complete(macs)
                adapter.closeProfileProxy(profile, proxy)
            }

            override fun onServiceDisconnected(profile: Int) {
                if (!deferred.isCompleted) deferred.complete(emptyList())
            }
        }

        if (!adapter.getProfileProxy(context, listener, BluetoothProfile.A2DP)) {
            return@withContext emptyList()
        }

        withTimeoutOrNull(3_000L) { deferred.await() } ?: emptyList()
    }
}
