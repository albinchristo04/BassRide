package com.velcuri.bassride.ui.permission

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.velcuri.bassride.ui.util.findActivity

@Composable
fun BluetoothPermissionScreen(
    onPermissionGranted: () -> Unit,
    onSkip: () -> Unit = onPermissionGranted
) {
    val context = LocalContext.current
    var showRationale by rememberSaveable { mutableStateOf(false) }
    var permissionDeniedPermanently by rememberSaveable { mutableStateOf(false) }

    val btPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        Manifest.permission.BLUETOOTH_CONNECT
    else
        Manifest.permission.BLUETOOTH

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onPermissionGranted()
        } else {
            val activity = context.findActivity()
            permissionDeniedPermanently = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, btPermission)
            showRationale = !permissionDeniedPermanently
        }
    }

    // Auto-launch permission request on first entry
    LaunchedEffect(Unit) {
        launcher.launch(btPermission)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (permissionDeniedPermanently)
                Icons.Default.BluetoothDisabled
            else
                Icons.Default.Bluetooth,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = if (permissionDeniedPermanently)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = if (permissionDeniedPermanently)
                "Bluetooth Access Blocked"
            else
                "Bluetooth Permission Needed",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = if (permissionDeniedPermanently)
                "BassRide needs Bluetooth access to detect your car and auto-load your EQ preset. " +
                    "Please enable it in Settings → Apps → BassRide → Permissions."
            else
                "BassRide uses Bluetooth to detect when you connect to your car and automatically " +
                    "load your saved EQ preset.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        if (permissionDeniedPermanently) {
            Button(onClick = {
                context.findActivity()?.let { activity ->
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    ).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    activity.startActivity(intent)
                }
            }) {
                Text("Open Settings")
            }
        } else {
            Button(onClick = { launcher.launch(btPermission) }) {
                Text("Grant Permission")
            }
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onSkip) {
            Text("Skip for now")
        }
    }
}
