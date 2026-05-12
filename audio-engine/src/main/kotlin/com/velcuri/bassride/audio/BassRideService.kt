package com.velcuri.bassride.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.velcuri.bassride.audio.domain.usecase.AutoSwitchUseCase
import com.velcuri.bassride.bluetooth.BluetoothEvent
import com.velcuri.bassride.bluetooth.BluetoothReceiver
import com.velcuri.bassride.bluetooth.domain.model.BluetoothDeviceInfo
import com.velcuri.bassride.bluetooth.domain.repository.BluetoothRepository
import com.velcuri.bassride.bluetooth.domain.usecase.GetConnectedA2dpDevicesUseCase
import com.velcuri.bassride.data.dao.PresetDao
import com.velcuri.bassride.data.entity.PresetEntity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val NOTIFICATION_ID = 1001
private const val NOTIFICATION_ID_NEW_DEVICE = 1002
private const val CHANNEL_ID = "bassride_eq_channel"
private const val CHANNEL_ID_SETUP = "bassride_setup_channel"

@AndroidEntryPoint
class BassRideService : Service() {

    @Inject lateinit var eqEngine: BassRideDspEngine
    @Inject lateinit var audioSessionManager: AudioSessionManager
    @Inject lateinit var bluetoothRepository: BluetoothRepository
    @Inject lateinit var presetDao: PresetDao
    @Inject lateinit var autoSwitchUseCase: AutoSwitchUseCase
    @Inject lateinit var getConnectedA2dpDevicesUseCase: GetConnectedA2dpDevicesUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Ordered list of all preset IDs for widget Prev/Next navigation. */
    private var orderedPresetIds: List<Long> = emptyList()
    private var activePresetId: Long? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        val notification = buildEqNotification("EQ Active")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        serviceScope.launch { eqEngine.initialize() }
        audioSessionManager.startListening()
        observeBluetoothEvents()
        observePresetsForWidget()

        // Edge case: Bluetooth device was already connected before the service started.
        // Give the EQ engine a moment to initialize, then check A2DP connections.
        serviceScope.launch {
            delay(1_500)
            checkAlreadyConnectedDevices()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle Prev/Next preset commands from the home-screen widget
        when (intent?.action) {
            WIDGET_ACTION_SWITCH_PRESET -> {
                val direction = intent.getStringExtra(EXTRA_WIDGET_DIRECTION)
                serviceScope.launch { handleWidgetPresetSwitch(direction) }
            }
        }
        return START_STICKY
    }

    private fun observeBluetoothEvents() {
        serviceScope.launch {
            BluetoothReceiver.events.collect { event ->
                when (event) {
                    is BluetoothEvent.DeviceConnected  -> handleDeviceConnected(event.device)
                    is BluetoothEvent.DeviceDisconnected -> handleDeviceDisconnected(event.device)
                }
            }
        }
    }

    /** Keeps [orderedPresetIds] in sync for widget prev/next navigation. */
    private fun observePresetsForWidget() {
        serviceScope.launch {
            presetDao.observeAll().collect { presets ->
                orderedPresetIds = presets.map { it.id }
            }
        }
    }

    private suspend fun checkAlreadyConnectedDevices() {
        val macs = getConnectedA2dpDevicesUseCase()
        macs.forEach { mac ->
            val name = bluetoothRepository.getDevice(mac)?.name ?: "Car Audio"
            handleDeviceConnected(BluetoothDeviceInfo(mac, name))
        }
    }

    private suspend fun handleDeviceConnected(device: BluetoothDeviceInfo) {
        // Bluetooth reconnect can drop AudioFlinger sessions — reinitialize if needed
        eqEngine.reinitializeIfNeeded()

        when (val result = autoSwitchUseCase(device.macAddress)) {
            is AutoSwitchUseCase.Result.Applied -> {
                activePresetId = result.presetId
                applyAndAnnounce(result.presetName)
            }
            is AutoSwitchUseCase.Result.DeviceUnknown -> {
                // First time seeing this device — save it and prompt the user
                bluetoothRepository.saveDevice(device.macAddress, device.name)
                showNewDeviceNotification(device.name)
            }
            is AutoSwitchUseCase.Result.AutoSwitchDisabled -> {
                // Device is known but auto-switch is off — EQ stays as-is, update notification
                updateEqNotification("EQ Active (manual)")
            }
            is AutoSwitchUseCase.Result.NoPresetLinked -> {
                // Device is known but has no preset linked — prompt
                showNewDeviceNotification(device.name)
            }
        }
    }

    private suspend fun handleDeviceDisconnected(device: BluetoothDeviceInfo) {
        eqEngine.applyPreset(FLAT_PRESET)
        activePresetId = null
        updateEqNotification("EQ Active")
        notifyWidget("Flat", eqActive = false)
    }

    private suspend fun handleWidgetPresetSwitch(direction: String?) {
        if (direction != null) cycleBoundPreset(direction)
    }

    private suspend fun cycleBoundPreset(direction: String) {
        val ids = orderedPresetIds
        if (ids.isEmpty()) return
        val currentIndex = ids.indexOf(activePresetId).takeIf { it >= 0 } ?: 0
        val nextIndex = when (direction) {
            DIRECTION_PREV -> (currentIndex - 1 + ids.size) % ids.size
            DIRECTION_NEXT -> (currentIndex + 1) % ids.size
            else           -> return
        }
        val preset = presetDao.getById(ids[nextIndex]) ?: return
        eqEngine.applyPreset(preset)
        applyAndAnnounce(preset.name)
        activePresetId = preset.id
    }

    /** Updates the notification and widget after a preset has already been applied. */
    private fun applyAndAnnounce(presetName: String) {
        updateEqNotification("Preset: $presetName")
        notifyWidget(presetName, eqActive = true)
    }

    /** Pushes an update to the home-screen widget via an explicit broadcast. */
    private fun notifyWidget(presetName: String, eqActive: Boolean) {
        sendBroadcast(Intent(ACTION_WIDGET_UPDATE).apply {
            setPackage(packageName)
            putExtra(EXTRA_PRESET_NAME, presetName)
            putExtra(EXTRA_EQ_ACTIVE, eqActive)
        })
    }

    // --- Notifications ---

    private fun updateEqNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildEqNotification(text))
    }

    private fun buildEqNotification(contentText: String): Notification {
        val openIntent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.let { PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE) }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("BassRide")
            .setContentText(contentText)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    /**
     * Shown once when a brand-new Bluetooth device is detected.
     * Tapping it opens the Devices screen so the user can link a preset.
     */
    private fun showNewDeviceNotification(deviceName: String) {
        val openDevicesIntent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.apply { putExtra(EXTRA_OPEN_DEVICES_SCREEN, true) }
            ?.let {
                PendingIntent.getActivity(
                    this, NOTIFICATION_ID_NEW_DEVICE, it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_SETUP)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New car detected: $deviceName")
            .setContentText("Tap to link an EQ preset to this device.")
            .setContentIntent(openDevicesIntent)
            .setAutoCancel(true)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID_NEW_DEVICE, notification)
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "BassRide EQ", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "EQ active while Bluetooth is connected" }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID_SETUP, "Device Setup", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Prompts to link a preset to a new Bluetooth device" }
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        audioSessionManager.stopListening()
        eqEngine.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_OPEN_DEVICES_SCREEN = "open_devices_screen"

        // Widget communication constants (also consumed by WidgetPresetSwitchReceiver in :app)
        const val WIDGET_ACTION_SWITCH_PRESET = "com.velcuri.bassride.SWITCH_PRESET"
        const val EXTRA_WIDGET_DIRECTION      = "direction"
        const val DIRECTION_PREV              = "prev"
        const val DIRECTION_NEXT              = "next"

        const val ACTION_WIDGET_UPDATE = "com.velcuri.bassride.WIDGET_UPDATE"
        const val EXTRA_PRESET_NAME    = "preset_name"
        const val EXTRA_EQ_ACTIVE      = "eq_active"

        private val FLAT_PRESET = PresetEntity(id = -1L, name = "Flat", isBuiltIn = true)
    }
}
