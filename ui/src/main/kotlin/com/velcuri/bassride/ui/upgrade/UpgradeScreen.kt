package com.velcuri.bassride.ui.upgrade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeScreen(
    onBack: () -> Unit,
    viewModel: UpgradeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Go Pro") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "BassRide Pro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "One-time purchase. No subscriptions. No ads.",
                style = MaterialTheme.typography.bodyMedium
            )

            // Feature list
            val proFeatures = listOf(
                "10-band parametric equalizer",
                "Unlimited saved presets",
                "Auto-load preset when car BT connects",
                "Per-device EQ profiles",
                "Road Noise compensation profile",
                "Bass Booster + Virtualizer effects",
                "Quick-access floating widget",
                "EQ stays active when app is closed"
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                proFeatures.forEach { feature ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(feature, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            when (val state = uiState) {
                is UpgradeUiState.Loading -> Button(onClick = {}, enabled = false) {
                    Text("Loading...")
                }
                is UpgradeUiState.AlreadyPro -> {
                    Text(
                        "You have Pro!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is UpgradeUiState.Available -> {
                    Button(
                        onClick = viewModel::startPurchase,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Unlock Pro — ${state.price}")
                    }
                    TextButton(onClick = viewModel::restorePurchases) {
                        Text("Restore Purchase")
                    }
                }
                is UpgradeUiState.Unavailable -> {
                    Text(
                        "In-app purchase unavailable",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Pro can only be purchased through the Google Play Store. " +
                        "If you installed this APK directly, please install from Play Store to unlock Pro.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = viewModel::restorePurchases) {
                        Text("Restore Existing Purchase")
                    }
                }
                is UpgradeUiState.Error -> {
                    Text(
                        state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = {
                        viewModel.clearError()
                        viewModel.startPurchase()
                    }) {
                        Text("Try Again")
                    }
                    TextButton(onClick = viewModel::clearError) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}
