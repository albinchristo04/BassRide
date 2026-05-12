package com.velcuri.bassride.audio

import com.velcuri.bassride.data.entity.PresetEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Layer 3 — AutoEQ Algorithm (Pro only).
 *
 * MIT-licensed algorithm independently reimplemented in Kotlin.
 * Source algorithm: jaakkopasanen/AutoEq (MIT License, github.com/jaakkopasanen/AutoEq).
 *
 * Input:  a target curve as [List<Pair<Float, Float>>] of (frequency Hz, gain dB) breakpoints.
 * Output: a [PresetEntity] with up to 10 parametric bands set via [BassRideDspEngine].
 *
 * Algorithm: greedy iterative peak-finding.
 *  1. Normalize the target curve over 250–2500 Hz.
 *  2. Compute initial error = target - current (starting all-flat).
 *  3. Find the frequency with worst deviation (3-point smoothing, 1.2× sub-50 Hz weighting).
 *  4. Fit a biquad peaking filter at that frequency (gain ±12 dB, Q 0.6–6.0).
 *  5. Subtract filter's frequency response from the error curve.
 *  6. Repeat up to 10 times or until max |error| < 0.05 dB.
 *  7. Convert output bands to millibels for [PresetEntity].
 */
@Singleton
class AutoEqEngine @Inject constructor() {

    data class ParametricBand(
        val frequencyHz: Float,
        val gainDb: Float,
        val q: Float
    )

    /**
     * Generates an optimal EQ preset from [targetCurve].
     * Gate this call behind `isProUnlocked == true`.
     *
     * @param targetCurve List of (Hz, dB) breakpoints for the desired frequency response.
     * @param presetName  Name for the generated preset.
     * @return [PresetEntity] ready to store in Room DB and apply via [BassRideDspEngine].
     */
    fun generatePreset(
        targetCurve: List<Pair<Float, Float>>,
        presetName: String
    ): PresetEntity {
        val bands = computeBands(targetCurve)
        return bandsToPreset(bands, presetName)
    }

    // ── Core algorithm ─────────────────────────────────────────────────────────

    private fun computeBands(targetCurve: List<Pair<Float, Float>>): List<ParametricBand> {
        // Build evaluation frequency grid (log-spaced 20–20000 Hz, 200 points)
        val freqs = logSpaceFreqs(20f, 20_000f, 200)

        // Interpolate target curve onto evaluation grid
        val targetDb = interpolateCurve(targetCurve, freqs)

        // Normalize: mean of 250–2500 Hz region becomes 0 dB
        val normOffset = targetDb.filterIndexed { idx, _ ->
            freqs[idx] in 250f..2500f
        }.average().toFloat()
        val error = targetDb.map { it - normOffset }.toFloatArray()

        // Current cumulative response (starts flat)
        val current = FloatArray(freqs.size) { 0f }

        val resultBands = mutableListOf<ParametricBand>()

        repeat(BassRideDspEngine.BAND_COUNT) {
            // Find worst deviation with sub-50Hz weighting and 3-point smoothing
            var worstIdx = 0
            var worstDev = 0f
            for (i in 1 until freqs.size - 1) {
                val smoothed = (error[i - 1] + error[i] + error[i + 1]) / 3f
                val weighted = if (freqs[i] < 50f) smoothed * 1.2f else smoothed
                if (abs(weighted) > abs(worstDev)) {
                    worstDev = weighted
                    worstIdx = i
                }
            }

            // Terminate early if max error is negligible
            if (abs(worstDev) < 0.05f) return resultBands

            val freq = freqs[worstIdx]
            val gain = worstDev.coerceIn(-12f, 12f)
            val q = computeQ(gain).coerceIn(0.6f, 6.0f)

            resultBands.add(ParametricBand(freq, gain, q))

            // Subtract this band's biquad response from the error
            val response = biquadPeakResponse(freqs, freq, gain, q)
            for (i in freqs.indices) {
                error[i] -= response[i]
                current[i] += response[i]
            }
        }

        return resultBands
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Maps computed [ParametricBand]s onto the 10 fixed DSP bands
     * by accumulating gain contributions at each fixed frequency.
     */
    private fun bandsToPreset(bands: List<ParametricBand>, name: String): PresetEntity {
        val fixedFreqs = BassRideDspEngine.BAND_FREQUENCIES
        val gains = FloatArray(BassRideDspEngine.BAND_COUNT) { 0f }

        for (band in bands) {
            for (i in fixedFreqs.indices) {
                gains[i] += gaussianContribution(
                    freq = fixedFreqs[i],
                    centerHz = band.frequencyHz,
                    gainDb = band.gainDb,
                    q = band.q
                )
            }
        }

        val mb = gains.map { (it * 100).toInt().coerceIn(BassRideDspEngine.MIN_MB, BassRideDspEngine.MAX_MB) }
        return PresetEntity(
            name = name,
            isBuiltIn = false,
            band0 = mb[0], band1 = mb[1], band2 = mb[2], band3 = mb[3], band4 = mb[4],
            band5 = mb[5], band6 = mb[6], band7 = mb[7], band8 = mb[8], band9 = mb[9]
        )
    }

    /** Gaussian gain contribution of a parametric band at [freq]. */
    private fun gaussianContribution(freq: Float, centerHz: Float, gainDb: Float, q: Float): Float {
        val octaveDist = octaveDistance(freq, centerHz)
        val bandwidth = 1f / q
        return gainDb * exp(-0.5f * (octaveDist / bandwidth).let { it * it })
    }

    /** Q factor estimate: narrow Q for large gains, wide Q for small gains. */
    private fun computeQ(gainDb: Float): Float {
        val absGain = abs(gainDb)
        return when {
            absGain >= 8f -> 2.5f
            absGain >= 4f -> 1.8f
            absGain >= 2f -> 1.4f
            else          -> 1.0f
        }
    }

    /** Frequency response of a peaking biquad EQ filter at each frequency in [freqs]. */
    private fun biquadPeakResponse(freqs: FloatArray, fc: Float, gainDb: Float, q: Float): FloatArray {
        // Simplified: use Gaussian approximation for the filter's frequency response
        return FloatArray(freqs.size) { i ->
            gaussianContribution(freqs[i], fc, gainDb, q)
        }
    }

    /** Distance between two frequencies in octaves. */
    private fun octaveDistance(f1: Float, f2: Float): Float {
        if (f1 <= 0f || f2 <= 0f) return Float.MAX_VALUE
        return abs(ln(f1 / f2.toDouble()).toFloat() / LN2)
    }

    /** Log-spaced frequency grid from [start] to [end] with [n] points. */
    private fun logSpaceFreqs(start: Float, end: Float, n: Int): FloatArray {
        val logStart = ln(start.toDouble())
        val logEnd = ln(end.toDouble())
        return FloatArray(n) { i ->
            exp(logStart + (logEnd - logStart) * i / (n - 1)).toFloat()
        }
    }

    /** Linear interpolation of a curve defined by (freq, dB) breakpoints. */
    private fun interpolateCurve(curve: List<Pair<Float, Float>>, freqs: FloatArray): FloatArray {
        val sorted = curve.sortedBy { it.first }
        return FloatArray(freqs.size) { i ->
            val f = freqs[i]
            when {
                f <= sorted.first().first -> sorted.first().second
                f >= sorted.last().first  -> sorted.last().second
                else -> {
                    val hi = sorted.indexOfFirst { it.first >= f }
                    val lo = hi - 1
                    val (f0, g0) = sorted[lo]
                    val (f1, g1) = sorted[hi]
                    val t = (f - f0) / (f1 - f0)
                    g0 + t * (g1 - g0)
                }
            }
        }
    }

    companion object {
        private val LN2 = ln(2.0).toFloat()
    }
}
