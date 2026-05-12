package com.velcuri.bassride.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onUpgradeClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            ListItem(
                headlineContent = { Text("Auto-switch presets") },
                supportingContent = { Text("Load preset when car Bluetooth connects") },
                trailingContent = {
                    Switch(
                        checked = uiState.autoSwitchEnabled,
                        onCheckedChange = viewModel::setAutoSwitchEnabled
                    )
                }
            )
            HorizontalDivider()

            if (!uiState.isProUnlocked) {
                ListItem(
                    headlineContent = { Text("BassRide Pro") },
                    supportingContent = { Text("10-band EQ, unlimited presets, auto-switch") },
                    trailingContent = {
                        TextButton(onClick = onUpgradeClick) { Text("Upgrade") }
                    }
                )
                HorizontalDivider()
            }

            ListItem(
                headlineContent = { Text("Version") },
                trailingContent = { Text("1.0.0") }
            )
        }
    }
}
