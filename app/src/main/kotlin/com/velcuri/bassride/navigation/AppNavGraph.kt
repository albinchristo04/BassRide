package com.velcuri.bassride.navigation

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.velcuri.bassride.ui.devices.DevicesScreen
import com.velcuri.bassride.ui.eq.EqScreen
import com.velcuri.bassride.ui.onboarding.OnboardingScreen
import com.velcuri.bassride.ui.onboarding.OnboardingViewModel
import com.velcuri.bassride.ui.permission.BluetoothPermissionScreen
import com.velcuri.bassride.ui.presets.PresetsScreen
import com.velcuri.bassride.ui.settings.SettingsScreen
import com.velcuri.bassride.ui.upgrade.UpgradeScreen

sealed class Screen(val route: String, val label: String) {
    data object Onboarding : Screen("onboarding", "Welcome")
    data object Eq       : Screen("eq",       "Equalizer")
    data object Presets  : Screen("presets",  "Presets")
    data object Devices  : Screen("devices",  "Device")
    data object Settings : Screen("settings", "More")
    data object Upgrade  : Screen("upgrade",  "Upgrade")
    data object BluetoothPermission : Screen("bluetooth_permission", "Bluetooth")
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppNavGraph(startOnDevicesScreen: Boolean = false) {
    // Eagerly read onboarding flag — null while the DB query is in flight (< 1 frame)
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val hasCompletedOnboarding by onboardingViewModel.hasCompletedOnboarding
        .collectAsStateWithLifecycle()

    if (hasCompletedOnboarding == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val btPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Manifest.permission.BLUETOOTH_CONNECT
    } else {
        Manifest.permission.BLUETOOTH
    }
    val bluetoothPermissionState = rememberPermissionState(btPermission)

    val fullScreenRoutes = setOf(Screen.Onboarding.route, Screen.BluetoothPermission.route)
    val showBottomBar = currentDestination?.route !in fullScreenRoutes

    val startDestination = when {
        hasCompletedOnboarding == false -> Screen.Onboarding.route
        !bluetoothPermissionState.status.isGranted -> Screen.BluetoothPermission.route
        startOnDevicesScreen -> Screen.Devices.route
        else -> Screen.Eq.route
    }

    val bottomNavItems = listOf(
        Screen.Eq to Icons.Default.Equalizer,
        Screen.Presets to Icons.Default.LibraryMusic,
        Screen.Devices to Icons.Default.Bluetooth,
        Screen.Settings to Icons.Default.MoreHoriz,
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { (screen, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinished = {
                        val dest = if (bluetoothPermissionState.status.isGranted)
                            Screen.Eq.route
                        else
                            Screen.BluetoothPermission.route
                        navController.navigate(dest) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.BluetoothPermission.route) {
                BluetoothPermissionScreen(
                    onPermissionGranted = {
                        navController.navigate(Screen.Eq.route) {
                            popUpTo(Screen.BluetoothPermission.route) { inclusive = true }
                        }
                    },
                    onSkip = {
                        navController.navigate(Screen.Eq.route) {
                            popUpTo(Screen.BluetoothPermission.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Eq.route) {
                EqScreen(onUpgradeClick = { navController.navigate(Screen.Upgrade.route) })
            }
            composable(Screen.Presets.route) {
                PresetsScreen(onUpgradeClick = { navController.navigate(Screen.Upgrade.route) })
            }
            composable(Screen.Devices.route) {
                DevicesScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onUpgradeClick = { navController.navigate(Screen.Upgrade.route) })
            }
            composable(Screen.Upgrade.route) {
                UpgradeScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
