package com.velcuri.bassride.ui.upgrade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velcuri.bassride.billing.domain.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpgradeViewModel @Inject constructor(
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    private val _loading = MutableStateFlow(false)
    private val _price = MutableStateFlow<String?>(null)

    val uiState: StateFlow<UpgradeUiState> = combine(
        billingRepository.isProUnlocked,
        _loading,
        _error,
        _price
    ) { isPro, loading, error, price ->
        when {
            isPro         -> UpgradeUiState.AlreadyPro
            error != null -> UpgradeUiState.Error(error)
            loading       -> UpgradeUiState.Loading
            price == null -> UpgradeUiState.Unavailable   // product not on Play yet
            else          -> UpgradeUiState.Available(price)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UpgradeUiState.Loading)

    init {
        viewModelScope.launch {
            _price.value = billingRepository.getProPrice()
        }
    }

    fun startPurchase() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                billingRepository.launchPurchaseFlow()
            } catch (e: Exception) {
                _error.value = e.message ?: "Purchase failed. Please try again."
            } finally {
                _loading.value = false
            }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                billingRepository.restorePurchases()
            } catch (e: Exception) {
                _error.value = "Restore failed. Check your internet connection."
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
