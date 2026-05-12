package com.velcuri.bassride.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Virtualizer
import com.velcuri.bassride.data.entity.PresetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Layer 1 — DynamicsProcessing API (Core Engine).
 *
 * Wraps [DynamicsProcessing] (API 28+) to provide true 10-band parametric EQ at
 * exact fixed frequencies — no Gaussian mapping needed.
 *
 * Sessions are managed by [AudioSessionManager]:
 *  - On audio session open → [attachToSession]
 *  - On audio session close → [detachFromSession]
 *  - On service destroy → [release]
 *
 * All DynamicsProcessing calls execute on [Dispatchers.IO].
 */
@Singleton
class BassRideDspEngine @Inject constructor() {

    companion object {
        /** Exact parametric band center frequencies in Hz. */
        val BAND_FREQUENCIES = floatArrayOf(
            32f, 64f, 125f, 250f, 500f, 1_000f, 2_000f, 4_000f, 8_000f, 16_000f
        )
        const val BAND_COUNT = 10
        /** ±12 dB in millibels */
        const val MIN_MB = -1200
        const val MAX_MB = 1200
    }

    // ── Session maps ───────────────────────────────────────────────────────────
    private val sessionLock = Mutex()
    private val dspSessions      = LinkedHashMap<Int, DynamicsProcessing>()
    private val bassBoostSessions = LinkedHashMap<Int, BassBoost>()
    private val virtualizerSessions = LinkedHashMap<Int, Virtualizer>()

    // ── Shared state ───────────────────────────────────────────────────────────
    private val currentGainsMb = IntArray(BAND_COUNT) { 0 }
    private var currentBassBoostEnabled   = false
    private var currentBassBoostStrength  = 0
    private var currentVirtualizerEnabled = false
    private var currentVirtualizerStrength = 500

    // ── StateFlows ─────────────────────────────────────────────────────────────
    private val _isActive         = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _bandCount        = MutableStateFlow(BAND_COUNT)
    val bandCount: StateFlow<Int>  = _bandCount.asStateFlow()

    private val _bandLevels       = MutableStateFlow(IntArray(BAND_COUNT) { 0 })
    val bandLevels: StateFlow<IntArray> = _bandLevels.asStateFlow()

    private val _bandRange        = MutableStateFlow(Pair(MIN_MB, MAX_MB))
    val bandRange: StateFlow<Pair<Int, Int>> = _bandRange.asStateFlow()

    private val _isBassBoostEnabled  = MutableStateFlow(false)
    val isBassBoostEnabled: StateFlow<Boolean> = _isBassBoostEnabled.asStateFlow()

    private val _bassBoostStrength   = MutableStateFlow(0)
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength.asStateFlow()

    private val _isVirtualizerEnabled = MutableStateFlow(false)
    val isVirtualizerEnabled: StateFlow<Boolean> = _isVirtualizerEnabled.asStateFlow()

    private val _initError        = MutableStateFlow<String?>(null)
    val initError: StateFlow<String?> = _initError.asStateFlow()

    // ── Init ───────────────────────────────────────────────────────────────────

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching<Unit> {
            _bandCount.value  = BAND_COUNT
            _bandRange.value  = Pair(MIN_MB, MAX_MB)
            _bandLevels.value = currentGainsMb.copyOf()
            _isActive.value   = true
            _initError.value  = null
        }.also { result ->
            if (result.isFailure) {
                _initError.value = "DSP engine initialization failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    suspend fun reinitializeIfNeeded(): Result<Unit> {
        if (_isActive.value) return Result.success(Unit)
        return initialize()
    }

    // ── Session management ─────────────────────────────────────────────────────

    /** Called by [AudioSessionManager] when an audio session opens. */
    suspend fun attachToSession(sessionId: Int): Unit = withContext(Dispatchers.IO) {
        sessionLock.withLock {
            if (dspSessions.containsKey(sessionId)) return@withContext
            runCatching {
                val dsp = createDspForSession(sessionId)
                writeBandsToDsp(dsp)
                dsp.enabled = true
                dspSessions[sessionId] = dsp
            }
            runCatching {
                val bb = BassBoost(0, sessionId)
                bb.enabled = currentBassBoostEnabled
                if (currentBassBoostEnabled) {
                    bb.setStrength(currentBassBoostStrength.toShort().coerceIn(0, 1000))
                }
                bassBoostSessions[sessionId] = bb
            }
            runCatching {
                val virt = Virtualizer(0, sessionId)
                virt.enabled = currentVirtualizerEnabled
                if (currentVirtualizerEnabled) {
                    virt.setStrength(currentVirtualizerStrength.toShort().coerceIn(0, 1000))
                }
                virtualizerSessions[sessionId] = virt
            }
        }
    }

    /** Called by [AudioSessionManager] when an audio session closes. */
    suspend fun detachFromSession(sessionId: Int): Unit = withContext(Dispatchers.IO) {
        sessionLock.withLock {
            runCatching { dspSessions.remove(sessionId)?.release() }
            runCatching { bassBoostSessions.remove(sessionId)?.release() }
            runCatching { virtualizerSessions.remove(sessionId)?.release() }
        }
    }

    // ── Preset / band application ──────────────────────────────────────────────

    suspend fun applyPreset(preset: PresetEntity): Unit = withContext(Dispatchers.IO) {
        val bands = preset.bands()
        for (i in 0 until BAND_COUNT) {
            currentGainsMb[i] = bands.getOrElse(i) { 0 }.coerceIn(MIN_MB, MAX_MB)
        }
        _bandLevels.value = currentGainsMb.copyOf()

        sessionLock.withLock {
            for (dsp in dspSessions.values) {
                runCatching { writeBandsToDsp(dsp) }
            }
        }

        val bbStrength = preset.bassBoostStrength.coerceIn(0, 1000)
        setBassBoostInternal(bbStrength > 0, bbStrength)
    }

    suspend fun setBandLevel(band: Int, levelMillibels: Int): Unit = withContext(Dispatchers.IO) {
        if (band < 0 || band >= BAND_COUNT) return@withContext
        val clamped = levelMillibels.coerceIn(MIN_MB, MAX_MB)
        currentGainsMb[band] = clamped
        _bandLevels.value = currentGainsMb.copyOf()

        sessionLock.withLock {
            for (dsp in dspSessions.values) {
                runCatching { writeSingleBandToDsp(dsp, band, clamped) }
            }
        }
    }

    suspend fun setBassBoostEnabled(enabled: Boolean, strength: Int = 500): Unit =
        withContext(Dispatchers.IO) { setBassBoostInternal(enabled, strength) }

    suspend fun setVirtualizerEnabled(enabled: Boolean, strength: Int = 500): Unit =
        withContext(Dispatchers.IO) {
            currentVirtualizerEnabled  = enabled
            currentVirtualizerStrength = strength
            _isVirtualizerEnabled.value = enabled
            sessionLock.withLock {
                for (virt in virtualizerSessions.values) {
                    runCatching {
                        virt.enabled = enabled
                        if (enabled) virt.setStrength(strength.toShort().coerceIn(0, 1000))
                    }
                }
            }
        }

    fun getBandCenterFreqs(): IntArray = IntArray(BAND_COUNT) { i -> BAND_FREQUENCIES[i].toInt() }

    /** Release all sessions. Call from [BassRideService.onDestroy]. */
    fun release() {
        for (dsp  in dspSessions.values)       runCatching { dsp.release()  }
        for (bb   in bassBoostSessions.values)  runCatching { bb.release()   }
        for (virt in virtualizerSessions.values) runCatching { virt.release() }
        dspSessions.clear()
        bassBoostSessions.clear()
        virtualizerSessions.clear()
        _isActive.value              = false
        _isBassBoostEnabled.value   = false
        _isVirtualizerEnabled.value = false
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Creates a [DynamicsProcessing] instance for [sessionId] with 10-band Pre-EQ
     * and a Limiter stage. Band frequencies and gains are applied afterward via
     * [writeBandsToDsp].
     */
    private fun createDspForSession(sessionId: Int): DynamicsProcessing {
        val config = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
            2,             // stereo
            true,          // preEqInUse
            BAND_COUNT,    // preEqBandCount
            false,         // mbcInUse
            0,             // mbcBandCount
            false,         // postEqInUse
            0,             // postEqBandCount
            true           // limiterInUse — prevents BT clipping
        ).build()
        return DynamicsProcessing(0, sessionId, config)
    }

    /**
     * Writes all 10 band gains from [currentGainsMb] to [dsp] via the Pre-EQ stage.
     * Uses [DynamicsProcessing.getPreEq] → [DynamicsProcessing.Eq.setBand] → [DynamicsProcessing.setPreEqAllChannelsTo].
     */
    private fun writeBandsToDsp(dsp: DynamicsProcessing) {
        val preEq: DynamicsProcessing.Eq = dsp.getPreEqByChannelIndex(0)
        for (i in 0 until BAND_COUNT) {
            val gainDb: Float = currentGainsMb[i].toFloat() / 100f
            val eqBand = DynamicsProcessing.EqBand(true, BAND_FREQUENCIES[i], gainDb)
            preEq.setBand(i, eqBand)
        }
        dsp.setPreEqAllChannelsTo(preEq)
    }

    /**
     * Updates a single band on [dsp] without touching other bands.
     */
    private fun writeSingleBandToDsp(dsp: DynamicsProcessing, band: Int, levelMb: Int) {
        val preEq: DynamicsProcessing.Eq = dsp.getPreEqByChannelIndex(0)
        val gainDb: Float = levelMb.toFloat() / 100f
        val eqBand = DynamicsProcessing.EqBand(true, BAND_FREQUENCIES[band], gainDb)
        preEq.setBand(band, eqBand)
        dsp.setPreEqAllChannelsTo(preEq)
    }

    private fun setBassBoostInternal(enabled: Boolean, strength: Int) {
        currentBassBoostEnabled  = enabled
        currentBassBoostStrength = if (enabled) strength else 0
        _isBassBoostEnabled.value  = enabled
        _bassBoostStrength.value   = currentBassBoostStrength
        for (bb in bassBoostSessions.values) {
            runCatching {
                bb.enabled = enabled
                if (enabled) bb.setStrength(strength.toShort().coerceIn(0, 1000))
            }
        }
    }
}
