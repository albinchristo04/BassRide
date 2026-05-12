package com.velcuri.bassride.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velcuri.bassride.audio.domain.usecase.ApplyPresetUseCase
import com.velcuri.bassride.data.dao.PresetDao
import com.velcuri.bassride.data.dao.UserSettingsDao
import com.velcuri.bassride.data.entity.PresetEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userSettingsDao: UserSettingsDao,
    private val presetDao: PresetDao,
    private val applyPresetUseCase: ApplyPresetUseCase
) : ViewModel() {

    /**
     * null  = DB query not yet resolved (show splash)
     * false = user hasn't completed onboarding
     * true  = onboarding already done
     */
    val hasCompletedOnboarding: StateFlow<Boolean?> = userSettingsDao.observe()
        .map { it?.hasCompletedOnboarding }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Built-in presets for the "choose a starter" page. */
    val builtInPresets: StateFlow<List<PresetEntity>> = presetDao.observeAll()
        .map { list -> list.filter { it.isBuiltIn } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Mark onboarding complete and optionally apply the user's chosen starter preset.
     * Safe to call multiple times.
     */
    fun completeOnboarding(selectedPreset: PresetEntity? = null) {
        viewModelScope.launch {
            if (selectedPreset != null) {
                applyPresetUseCase(selectedPreset)
            }
            userSettingsDao.markOnboardingComplete()
        }
    }
}
