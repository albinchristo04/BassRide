package com.velcuri.bassride.ui.eq

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velcuri.bassride.audio.BassRideDspEngine
import com.velcuri.bassride.audio.domain.usecase.ApplyPresetUseCase
import com.velcuri.bassride.audio.domain.usecase.LoadPresetsUseCase
import com.velcuri.bassride.audio.domain.usecase.SavePresetUseCase
import com.velcuri.bassride.billing.domain.BillingRepository
import com.velcuri.bassride.data.entity.PresetEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EqViewModel @Inject constructor(
    private val eqEngine: BassRideDspEngine,
    private val loadPresetsUseCase: LoadPresetsUseCase,
    private val applyPresetUseCase: ApplyPresetUseCase,
    private val savePresetUseCase: SavePresetUseCase,
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _activePreset = MutableStateFlow<PresetEntity?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private var debounceJob: Job? = null

    val uiState: StateFlow<EqUiState> = combine(
        eqEngine.isActive,
        eqEngine.bandLevels,
        eqEngine.bandCount,
        loadPresetsUseCase(),
        billingRepository.isProUnlocked,
        _activePreset,
        eqEngine.isBassBoostEnabled,
        eqEngine.bassBoostStrength,
        eqEngine.isVirtualizerEnabled,
        eqEngine.bandRange,
        eqEngine.initError
    ) { values ->
        val isActive          = values[0] as Boolean
        val bandLevels        = values[1] as IntArray
        val bandCount         = values[2] as Int
        @Suppress("UNCHECKED_CAST")
        val presets           = values[3] as List<PresetEntity>
        val isPro             = values[4] as Boolean
        val activePreset      = values[5] as? PresetEntity
        val bbEnabled         = values[6] as Boolean
        val bbStrength        = values[7] as Int
        val virtEnabled       = values[8] as Boolean
        @Suppress("UNCHECKED_CAST")
        val bandRange         = values[9] as Pair<Int, Int>
        val initError         = values[10] as? String

        // Surface AudioEffect initialisation failures immediately
        if (initError != null) return@combine EqUiState.Error(initError)
        if (!isActive) return@combine EqUiState.Loading

        EqUiState.Ready(
            bandLevels           = bandLevels,
            bandCount            = bandCount,
            minMillibels         = bandRange.first,
            maxMillibels         = bandRange.second,
            activePreset         = activePreset,
            presets              = presets,
            isProUnlocked        = isPro,
            isBassBoostEnabled   = bbEnabled,
            bassBoostStrength    = bbStrength,
            isVirtualizerEnabled = virtEnabled
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EqUiState.Loading)

    val errorMessage: StateFlow<String?> = _errorMessage

    fun onBandChanged(band: Int, levelMillibels: Int) {
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(100)
            eqEngine.setBandLevel(band, levelMillibels)
            _activePreset.value = null // Mark preset as modified
        }
    }

    fun onPresetSelected(preset: PresetEntity) {
        viewModelScope.launch {
            applyPresetUseCase(preset)
            _activePreset.value = preset
        }
    }

    fun setBassBoostEnabled(enabled: Boolean, strength: Int = 500) {
        viewModelScope.launch { eqEngine.setBassBoostEnabled(enabled, strength) }
    }

    fun setVirtualizerEnabled(enabled: Boolean, strength: Int = 500) {
        viewModelScope.launch { eqEngine.setVirtualizerEnabled(enabled, strength) }
    }

    fun saveCurrentAsPreset(name: String) {
        viewModelScope.launch {
            val current = uiState.value as? EqUiState.Ready ?: return@launch
            val levels = current.bandLevels
            val preset = PresetEntity(
                name = name,
                band0 = levels.getOrElse(0) { 0 },
                band1 = levels.getOrElse(1) { 0 },
                band2 = levels.getOrElse(2) { 0 },
                band3 = levels.getOrElse(3) { 0 },
                band4 = levels.getOrElse(4) { 0 },
                band5 = levels.getOrElse(5) { 0 },
                band6 = levels.getOrElse(6) { 0 },
                band7 = levels.getOrElse(7) { 0 },
                band8 = levels.getOrElse(8) { 0 },
                band9 = levels.getOrElse(9) { 0 },
                bassBoostStrength = if (current.isBassBoostEnabled) current.bassBoostStrength else 0
            )
            when (val result = savePresetUseCase(preset, current.isProUnlocked)) {
                is SavePresetUseCase.Result.Success ->
                    _activePreset.value = preset.copy(id = result.presetId)
                is SavePresetUseCase.Result.LimitReached ->
                    _errorMessage.value = "Upgrade to Pro for unlimited presets"
            }
        }
    }

    fun clearError() { _errorMessage.value = null }

    fun retryInitialization() {
        viewModelScope.launch { eqEngine.initialize() }
    }

    fun resetToFlat() {
        viewModelScope.launch {
            val ready = uiState.value as? EqUiState.Ready ?: return@launch
            repeat(ready.bandCount) { i -> eqEngine.setBandLevel(i, 0) }
            _activePreset.value = ready.presets.firstOrNull { it.isBuiltIn && it.name.equals("Flat", ignoreCase = true) }
        }
    }
}

