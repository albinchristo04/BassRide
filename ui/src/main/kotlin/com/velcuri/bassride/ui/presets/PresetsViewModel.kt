package com.velcuri.bassride.ui.presets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velcuri.bassride.audio.domain.usecase.ApplyPresetUseCase
import com.velcuri.bassride.audio.domain.usecase.LoadPresetsUseCase
import com.velcuri.bassride.audio.domain.repository.PresetRepository
import com.velcuri.bassride.billing.domain.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PresetsViewModel @Inject constructor(
    private val loadPresetsUseCase: LoadPresetsUseCase,
    private val applyPresetUseCase: ApplyPresetUseCase,
    private val presetRepository: PresetRepository,
    private val billingRepository: BillingRepository
) : ViewModel() {

    val uiState: StateFlow<PresetsUiState> = combine(
        loadPresetsUseCase(),
        billingRepository.isProUnlocked
    ) { presets, isPro ->
        val customCount = presets.count { !it.isBuiltIn }
        PresetsUiState.Ready(presets = presets, isProUnlocked = isPro, customPresetCount = customCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PresetsUiState.Loading)

    fun applyPreset(presetId: Long) {
        viewModelScope.launch {
            val preset = presetRepository.getById(presetId) ?: return@launch
            applyPresetUseCase(preset)
        }
    }

    fun deletePreset(presetId: Long) {
        viewModelScope.launch {
            presetRepository.delete(presetId)
        }
    }
}
