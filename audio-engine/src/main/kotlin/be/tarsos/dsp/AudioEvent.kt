// Minimal stub — replaces the be.tarsos.dsp:core dependency which is unavailable
// via any public Maven repository. Only the members used by TarsosEqEngine are
// provided; this is intentionally not a full TarsosDSP implementation.
package be.tarsos.dsp

/**
 * Represents a block of audio samples passed between DSP processors.
 *
 * Stub that exposes only the [floatBuffer] property used by [TarsosEqEngine][com.velcuri.bassride.audio.tarsos.TarsosEqEngine].
 */
class AudioEvent(
    /** The raw PCM samples for this buffer (mono or interleaved stereo). */
    val floatBuffer: FloatArray
)
