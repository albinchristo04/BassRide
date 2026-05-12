package com.velcuri.bassride.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.AudioEffect
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Layer 2 — Enhanced Session Detection.
 *
 * Standard mode (default): registers [BroadcastReceiver]s for
 * [AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION] and
 * [AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION].
 *
 * On session open → calls [BassRideDspEngine.attachToSession].
 * On session close → calls [BassRideDspEngine.detachFromSession].
 *
 * Enhanced mode (Pro — requires android.permission.DUMP) is wired separately via
 * [EnhancedSessionObserver] and is started/stopped by [BassRideService].
 */
@Singleton
class AudioSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dspEngine: BassRideDspEngine
) {

    sealed class SessionDetectionState {
        data object Idle : SessionDetectionState()
        data object StandardListening : SessionDetectionState()
        data class SessionsActive(val sessionCount: Int) : SessionDetectionState()
    }

    private val _state = MutableStateFlow<SessionDetectionState>(SessionDetectionState.Idle)
    val state: StateFlow<SessionDetectionState> = _state.asStateFlow()

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val sessionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
            if (sessionId == -1 || sessionId == 0) return

            when (intent.action) {
                AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                    managerScope.launch { dspEngine.attachToSession(sessionId) }
                }
                AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                    managerScope.launch { dspEngine.detachFromSession(sessionId) }
                }
            }
        }
    }

    /**
     * Starts listening for audio session broadcasts.
     * Call from [BassRideService.onCreate].
     */
    fun startListening() {
        val filter = IntentFilter().apply {
            addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        }
        context.registerReceiver(sessionReceiver, filter)
        _state.value = SessionDetectionState.StandardListening
    }

    /**
     * Stops listening and detaches all sessions.
     * Call from [BassRideService.onDestroy].
     */
    fun stopListening() {
        runCatching { context.unregisterReceiver(sessionReceiver) }
        _state.value = SessionDetectionState.Idle
        managerScope.cancel()
    }
}
