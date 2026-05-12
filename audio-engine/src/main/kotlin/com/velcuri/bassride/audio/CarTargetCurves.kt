package com.velcuri.bassride.audio

/**
 * Built-in car audio target curves for the AutoEQ engine.
 *
 * Each curve is a list of (frequency Hz, gain dB) breakpoints spanning the full audible range.
 * These are BassRide-original curve definitions based on psychoacoustic research and car audio physics.
 *
 * Used by [AutoEqEngine] to generate optimal EQ presets for a user's car.
 */
object CarTargetCurves {

    enum class CarTarget {
        HarmanCar, Flat, BassEmphasis, VocalClarity, RoadNoiseComp, NightDrive
    }

    /**
     * Harman Car Audio 2018 preferred in-car listening curve.
     * Research-backed preferred response for car audio environments.
     */
    val HarmanCar: List<Pair<Float, Float>> = listOf(
        20f to 6.0f,
        32f to 5.5f,
        63f to 5.0f,
        100f to 3.5f,
        200f to 1.5f,
        400f to 0.0f,
        800f to -0.5f,
        1_000f to -1.0f,
        2_000f to -2.0f,
        3_000f to -2.5f,
        4_000f to -3.0f,
        6_000f to -4.0f,
        8_000f to -4.5f,
        12_000f to -5.5f,
        16_000f to -7.0f,
        20_000f to -9.0f
    )

    /**
     * Completely flat / neutral reference curve.
     */
    val Flat: List<Pair<Float, Float>> = listOf(
        20f to 0f, 20_000f to 0f
    )

    /**
     * Harman curve plus a ~3 dB bass shelf below 80 Hz to compensate
     * for typical car cabin bass rolloff.
     */
    val BassEmphasis: List<Pair<Float, Float>> = listOf(
        20f to 9.0f,
        32f to 8.5f,
        63f to 7.5f,
        80f to 5.5f,
        100f to 3.5f,
        200f to 1.5f,
        400f to 0.0f,
        800f to -0.5f,
        1_000f to -1.0f,
        2_000f to -2.0f,
        4_000f to -3.0f,
        8_000f to -4.5f,
        16_000f to -7.0f
    )

    /**
     * Mid-forward curve optimized for voice, podcasts, and audiobooks in noisy cabin environments.
     */
    val VocalClarity: List<Pair<Float, Float>> = listOf(
        20f to 0f,
        80f to 0f,
        200f to 1.5f,
        500f to 2.0f,
        1_000f to 3.0f,
        2_000f to 3.5f,
        3_000f to 2.5f,
        4_000f to 1.5f,
        6_000f to 0.0f,
        10_000f to -2.0f,
        16_000f to -4.0f
    )

    /**
     * Boosts 2–6 kHz range to cut through highway wind and road noise.
     * Based on equal-loudness contour adjustments for ~75 dB SPL ambient noise.
     */
    val RoadNoiseComp: List<Pair<Float, Float>> = listOf(
        20f to 3.0f,
        60f to 2.5f,
        200f to 1.0f,
        500f to 0.5f,
        1_000f to 1.0f,
        2_000f to 3.5f,
        3_000f to 4.5f,
        4_000f to 5.0f,
        6_000f to 4.5f,
        8_000f to 2.0f,
        12_000f to 0.5f,
        16_000f to 0.0f
    )

    /**
     * Relaxed treble, warm enhanced bass for quiet night driving.
     */
    val NightDrive: List<Pair<Float, Float>> = listOf(
        20f to 5.0f,
        40f to 4.5f,
        80f to 3.5f,
        200f to 2.0f,
        500f to 0.5f,
        1_000f to 0.0f,
        2_000f to -1.0f,
        4_000f to -2.0f,
        6_000f to -3.5f,
        10_000f to -5.0f,
        16_000f to -6.5f
    )

    fun getCurveForTarget(target: CarTarget): List<Pair<Float, Float>> = when (target) {
        CarTarget.HarmanCar -> HarmanCar
        CarTarget.Flat -> Flat
        CarTarget.BassEmphasis -> BassEmphasis
        CarTarget.VocalClarity -> VocalClarity
        CarTarget.RoadNoiseComp -> RoadNoiseComp
        CarTarget.NightDrive -> NightDrive
    }
}
