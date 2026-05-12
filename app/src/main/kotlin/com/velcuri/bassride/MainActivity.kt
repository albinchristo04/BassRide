package com.velcuri.bassride

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.velcuri.bassride.audio.BassRideService
import com.velcuri.bassride.billing.domain.BillingRepository
import com.velcuri.bassride.navigation.AppNavGraph
import com.velcuri.bassride.ui.theme.BassRideTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var billingRepository: BillingRepository

    /** True when this activity was launched from the "new device" notification. */
    private var openDevicesScreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingRepository.setActivity(this)
        openDevicesScreen = intent?.getBooleanExtra(
            BassRideService.EXTRA_OPEN_DEVICES_SCREEN, false
        ) ?: false
        enableEdgeToEdge()
        setContent {
            BassRideTheme {
                AppNavGraph(startOnDevicesScreen = openDevicesScreen)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // App already running — honour the notification deep-link on subsequent taps
        if (intent.getBooleanExtra(BassRideService.EXTRA_OPEN_DEVICES_SCREEN, false)) {
            setIntent(intent)
            recreate()
        }
    }

    override fun onStart() {
        super.onStart()
        startBassRideService()
    }

    private fun startBassRideService() {
        val intent = Intent(this, BassRideService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
