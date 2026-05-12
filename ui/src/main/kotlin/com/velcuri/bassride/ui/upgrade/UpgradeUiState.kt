package com.velcuri.bassride.ui.upgrade

sealed class UpgradeUiState {
    data object Loading : UpgradeUiState()
    data object AlreadyPro : UpgradeUiState()
    data class Available(val price: String) : UpgradeUiState()
    /** Play Store is reachable but returned no product — app likely sideloaded. */
    data object Unavailable : UpgradeUiState()
    data class Error(val message: String) : UpgradeUiState()
}
