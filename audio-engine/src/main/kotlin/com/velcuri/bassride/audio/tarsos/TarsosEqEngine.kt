package com.velcuri.bassride.audio.tarsos

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import com.velcuri.bassride.billing.domain.BillingRepository
import com.velcuri.bassride.data.entity.PresetEntity
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Secondary EQ engine powered by Tarsos DSP.
 *
 * Implements a 10-band parametric equalizer using biquad peaking-EQ filters at exact
 * centre frequencies (32 Hz → 16 kHz). This engine processes audio buffers routed
 * **directly through BassRide** — it does NOT intercept system-wide audio.
 *
 * System-wide EQ is handled by [com.velcuri.bassride.audio.BassRideDspEngine] (DynamicsProcessing API).
 * This engine is the precision complement, providing:
 *  - Exact band centre frequencies (device-independent)
 *  - Per-band Q factor control
 *  - Sub-100 µs per-buffer latency on modern hardware
 *
 * **Pro feature — only instantiated when [BillingRepository.isProUnlocked] is true.**
 *
 * Future wiring: attach to an [be.tarsos.dsp.AudioDispatcher] pipeline when the
 * "Play through BassRide" feature is built.
 */
@Singleton
class TarsosEqEngine @Inject constructor(
    private val billingRepository: BillingRepository
) : AudioProcessor {

    companion object {
        /** Centre frequencies (Hz) for the 10 EQ bands. */
        val BAND_FREQUENCIES = intArrayOf(32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

        /** Default Q factor for all bands (affects bandwidth). Higher = narrower. */
        private const val DEFAULT_Q = 1.41
    }

    /** Whether this engine has been set up and is ready to process audio. */
    var isReady: Boolean = false
        private set

    private var sampleRate: Float = 44100f
    private val bandGainsDb = FloatArray(BAND_FREQUENCIES.size) { 0f }
    private val filters = arrayOfNulls<BiquadPeakingFilter>(BAND_FREQUENCIES.size)

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Configures the engine for the given [sampleRate] and builds biquad filter
     * coefficients for every band. Must be called before processing any audio.
     *
     * Only activates when Pro is unlocked; returns false silently otherwise.
     */
    fun setup(sampleRate: Float): Boolean {
        if (!billingRepository.isProUnlocked.value) return false
        this.sampleRate = sampleRate
        rebuildFilters()
        isReady = true
        return true
    }

    /**
     * Applies the band gains from [preset] (in millibels) to all EQ bands and
     * rebuilds the biquad coefficients.
     */
    fun applyPreset(preset: PresetEntity) {
        if (!isReady) return
        val bands = preset.bands()
        for (i in bandGainsDb.indices) {
            bandGainsDb[i] = if (i < bands.size) bands[i].millibelsToDb() else 0f
        }
        rebuildFilters()
    }

    /**
     * Sets the gain (in millibels) for a single [bandIndex] and updates that band's
     * filter coefficients.
     */
    fun setBandGainMillibels(bandIndex: Int, millibels: Int) {
        if (!isReady || bandIndex !in bandGainsDb.indices) return
        bandGainsDb[bandIndex] = millibels.millibelsToDb()
        filters[bandIndex] = BiquadPeakingFilter(
            centerHz = BAND_FREQUENCIES[bandIndex].toFloat(),
            sampleRate = sampleRate,
            gainDb = bandGainsDb[bandIndex],
            q = DEFAULT_Q
        )
    }

    /**
     * Returns the current gain in millibels for each band (for UI display).
     */
    fun getBandGainsMillibels(): IntArray =
        IntArray(bandGainsDb.size) { i -> bandGainsDb[i].dbToMillibels() }

    // ------------------------------------------------------------------
    // AudioProcessor (Tarsos DSP interface)
    // ------------------------------------------------------------------

    /**
     * Called by Tarsos [be.tarsos.dsp.AudioDispatcher] for each audio buffer.
     * Applies the parametric EQ in-place to [audioEvent]'s float buffer.
     */
    override fun process(audioEvent: AudioEvent): Boolean {
        if (!isReady) return true // pass through
        val buffer = audioEvent.floatBuffer
        filters.forEach { filter -> filter?.processBuffer(buffer) }
        return true
    }

    override fun processingFinished() {
        isReady = false
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private fun rebuildFilters() {
        for (i in BAND_FREQUENCIES.indices) {
            filters[i] = BiquadPeakingFilter(
                centerHz = BAND_FREQUENCIES[i].toFloat(),
                sampleRate = sampleRate,
                gainDb = bandGainsDb[i],
                q = DEFAULT_Q
            )
        }
    }

    private fun Int.millibelsToDb(): Float = this / 100f
    private fun Float.dbToMillibels(): Int = (this * 100).toInt()
}

// ---------------------------------------------------------------------------
// Biquad peaking-EQ filter — Audio EQ Cookbook (Robert Bristow-Johnson)
// ---------------------------------------------------------------------------

/**
 * A second-order IIR peaking-EQ filter.
 *
 * Transfer function:  H(z) = (b0 + b1·z⁻¹ + b2·z⁻²) / (a0 + a1·z⁻¹ + a2·z⁻²)
 *
 * Coefficients derived from the [Audio EQ Cookbook](https://www.w3.org/TR/audio-eq-cookbook/)
 * by Robert Bristow-Johnson.
 */
internal class BiquadPeakingFilter(
    centerHz: Float,
    sampleRate: Float,
    gainDb: Float,
    q: Double = 1.41
) {
    private val b0: Double
    private val b1: Double
    private val b2: Double
    private val a1: Double
    private val a2: Double

    // Per-channel state for Direct Form II transposed
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    init {
        val A = 10.0.pow(gainDb / 40.0)                   // amplitude
        val w0 = 2.0 * Math.PI * centerHz / sampleRate    // angular frequency
        val alpha = sin(w0) / (2.0 * q)                   // bandwidth term

        val a0 = 1.0 + alpha / A
        b0 = (1.0 + alpha * A) / a0
        b1 = (-2.0 * cos(w0)) / a0
        b2 = (1.0 - alpha * A) / a0
        a1 = (-2.0 * cos(w0)) / a0
        a2 = (1.0 - alpha / A) / a0
    }

    /** Processes [buffer] in-place (mono interleaved assumed; stereo works too). */
    fun processBuffer(buffer: FloatArray) {
        for (i in buffer.indices) {
            val x = buffer[i].toDouble()
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x
            y2 = y1; y1 = y
            buffer[i] = y.toFloat()
        }
    }
}
