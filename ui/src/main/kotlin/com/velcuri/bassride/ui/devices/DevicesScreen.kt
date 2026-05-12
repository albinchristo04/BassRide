package com.velcuri.bassride.ui.devices

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.velcuri.bassride.bluetooth.domain.model.BluetoothDeviceInfo
import com.velcuri.bassride.data.entity.BluetoothDeviceEntity
import com.velcuri.bassride.data.entity.PresetEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    viewModel: DevicesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Load paired devices when screen opens
    LaunchedEffect(Unit) { viewModel.refreshPairedDevices() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bluetooth Devices") },
                actions = {
                    IconButton(onClick = viewModel::refreshPairedDevices) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan paired devices")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is DevicesUiState.Loading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is DevicesUiState.Ready -> {
                if (state.devices.isEmpty() && state.importCandidates.isEmpty()) {
                    EmptyDevicesView(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                    )
                } else {
                    LazyColumn(modifier = Modifier.padding(padding)) {
                        // Saved device profiles
                        if (state.devices.isNotEmpty()) {
                            items(state.devices, key = { "saved_${it.macAddress}" }) { device ->
                                DeviceRow(
                                    device = device,
                                    presets = state.presets,
                                    onLinkPreset = { presetId -> viewModel.linkPreset(device.macAddress, presetId) },
                                    onAutoSwitchToggle = { enabled -> viewModel.setAutoSwitch(device.macAddress, enabled) },
                                    onDelete = { viewModel.deleteDevice(device.macAddress) }
                                )
                            }
                        }

                        // Import candidates — paired but not yet saved
                        if (state.importCandidates.isNotEmpty()) {
                            item {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text(
                                    "Paired Devices",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                            items(state.importCandidates, key = { "import_${it.macAddress}" }) { candidate ->
                                ImportCandidateRow(
                                    device = candidate,
                                    onImport = { viewModel.importDevice(candidate.macAddress, candidate.name) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDevicesView(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Bluetooth,
                contentDescription = null,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text("No devices detected yet", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Connect your car Bluetooth to get started,\nor tap ↺ to scan paired devices.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceRow(
    device: BluetoothDeviceEntity,
    presets: List<PresetEntity>,
    onLinkPreset: (Long?) -> Unit,
    onAutoSwitchToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var showPresetPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        ListItem(
            headlineContent = { Text(device.name) },
            supportingContent = {
                val linked = presets.find { it.id == device.linkedPresetId }
                Text(linked?.name ?: "No preset linked")
            },
            leadingContent = {
                Icon(Icons.Default.Bluetooth, contentDescription = "Bluetooth device ${device.name}")
            },
            trailingContent = {
                Switch(
                    checked = device.autoSwitchEnabled,
                    onCheckedChange = onAutoSwitchToggle,
                    modifier = Modifier.semantics {
                        contentDescription = "Auto-switch for ${device.name} ${if (device.autoSwitchEnabled) "on" else "off"}"
                    }
                )
            }
        )
        Row(modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, end = 8.dp)) {
            TextButton(onClick = { showPresetPicker = true }) {
                Text("Link preset")
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.semantics {
                    contentDescription = "Delete ${device.name} profile"
                }
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
            }
        }
    }

    if (showPresetPicker) {
        PresetPickerDialog(
            presets = presets,
            currentPresetId = device.linkedPresetId,
            onDismiss = { showPresetPicker = false },
            onSelect = { presetId ->
                onLinkPreset(presetId)
                showPresetPicker = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove Device") },
            text = { Text("Remove ${device.name}? Its linked preset will not be deleted.") },
            confirmButton = {
                Button(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ImportCandidateRow(
    device: BluetoothDeviceInfo,
    onImport: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        ListItem(
            headlineContent = { Text(device.name) },
            supportingContent = { Text(device.macAddress) },
            leadingContent = { Icon(Icons.Default.Bluetooth, contentDescription = null) },
            trailingContent = {
                IconButton(onClick = onImport) {
                    Icon(Icons.Default.Add, contentDescription = "Add device")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetPickerDialog(
    presets: List<PresetEntity>,
    currentPresetId: Long?,
    onDismiss: () -> Unit,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedPreset by remember {
        mutableStateOf(presets.find { it.id == currentPresetId })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link Preset") },
        text = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = selectedPreset?.name ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = { selectedPreset = null; expanded = false }
                    )
                    presets.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset.name) },
                            onClick = { selectedPreset = preset; expanded = false }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSelect(selectedPreset?.id) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
