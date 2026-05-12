package com.velcuri.bassride.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velcuri.bassride.billing.domain.BillingRepository
import com.velcuri.bassride.data.dao.UserSettingsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val autoSwitchEnabled: Boolean = true,
    val isProUnlocked: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSettingsDao: UserSettingsDao,
    private val billingRepository: BillingRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        userSettingsDao.observe(),
        billingRepository.isProUnlocked
    ) { settings, isPro ->
        SettingsUiState(
            autoSwitchEnabled = settings?.autoSwitchEnabled ?: true,
            isProUnlocked = isPro
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setAutoSwitchEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsDao.setAutoSwitchEnabled(enabled)
        }
    }
}
